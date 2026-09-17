import { isExhausted, readRateLimit, waitUntilReset } from './rate-limit.ts';

export const MAX_ATTEMPTS = 3;
export const SECONDARY_LIMIT_WAIT_MS = 60_000;
export const MAX_PACED_WAIT_MS = 15 * 60_000;

export type RetryDecision =
	| { kind: 'succeed' }
	| { kind: 'retry'; waitMs: number; reason: string }
	| { kind: 'pace'; waitMs: number; reason: string }
	| { kind: 'skip'; reason: string }
	| { kind: 'fail'; reason: string };

export type RetryContext = {
	status: number;
	headers: Headers;
	attempt: number;
	isCodeSearch: boolean;
	resource: string;
	maxAttempts?: number;
	now?: Date;
};

function exponentialWait(attempt: number): number {
	return 2 ** attempt * 1000;
}

export function decideRetry({
	status,
	headers,
	attempt,
	isCodeSearch,
	resource,
	maxAttempts = MAX_ATTEMPTS,
	now = new Date()
}: RetryContext): RetryDecision {
	if (status >= 200 && status < 300) return { kind: 'succeed' };
	if (status === 304) return { kind: 'succeed' };

	const hasAttemptsLeft = attempt < maxAttempts;
	const snapshot = readRateLimit(headers);

	if (snapshot.retryAfterMs !== null) {
		if (snapshot.retryAfterMs > MAX_PACED_WAIT_MS) {
			return {
				kind: 'fail',
				reason: `${resource} asked for a ${Math.round(snapshot.retryAfterMs / 60_000)} minute wait`
			};
		}

		return { kind: 'pace', waitMs: snapshot.retryAfterMs, reason: `Retry-After on ${resource}` };
	}

	if (isExhausted(snapshot) && (status === 403 || status === 429)) {
		const waitMs = waitUntilReset(snapshot.resetAt, now);

		if (waitMs > MAX_PACED_WAIT_MS) {
			return {
				kind: 'fail',
				reason: `${resource} rate limit does not reset for ${Math.round(waitMs / 60_000)} minutes`
			};
		}

		return {
			kind: 'pace',
			waitMs,
			reason: `primary rate limit exhausted on ${resource}`
		};
	}

	if (status === 404 && isCodeSearch) {
		if (!hasAttemptsLeft) {
			return { kind: 'fail', reason: `code search ${resource} kept returning 404 (throttled)` };
		}
		return {
			kind: 'retry',
			waitMs: exponentialWait(attempt),
			reason: `code search 404 on ${resource} is throttling, not absence`
		};
	}

	if (status === 403 || status === 429) {
		if (!hasAttemptsLeft) {
			return { kind: 'fail', reason: `${resource} hit a secondary rate limit ${attempt} times` };
		}
		return {
			kind: 'retry',
			waitMs: SECONDARY_LIMIT_WAIT_MS,
			reason: `secondary rate limit on ${resource}`
		};
	}

	if (status === 404 || status === 409 || status === 451) {
		return { kind: 'skip', reason: `${resource} is unavailable (${status})` };
	}

	if (status >= 500) {
		if (!hasAttemptsLeft) {
			return { kind: 'fail', reason: `${resource} returned ${status} on ${attempt} attempts` };
		}
		return {
			kind: 'retry',
			waitMs: exponentialWait(attempt),
			reason: `${resource} returned ${status}`
		};
	}

	return { kind: 'fail', reason: `${resource} returned unexpected status ${status}` };
}
