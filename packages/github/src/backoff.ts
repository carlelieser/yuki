import { isExhausted, readRateLimit, waitUntilReset } from './rate-limit.ts';

export const MAX_ATTEMPTS = 3;
export const SECONDARY_LIMIT_WAIT_MS = 60_000;

export type RetryDecision =
	| { kind: 'succeed' }
	| { kind: 'retry'; waitMs: number; reason: string }
	| { kind: 'pace'; waitMs: number; reason: string }
	| { kind: 'skip'; reason: string }
	| { kind: 'fail'; reason: string };

export type RateLimitPolicy = 'wait' | 'fail';

export type RetryContext = {
	status: number;
	headers: Headers;
	attempt: number;
	isCodeSearch: boolean;
	resource: string;
	maxAttempts?: number;
	onRateLimit?: RateLimitPolicy;
	now?: Date;
};

function exponentialWait(attempt: number): number {
	return 2 ** attempt * 1000;
}

export function decideRetry(context: RetryContext): RetryDecision {
	const decision = classify(context);
	if (context.onRateLimit !== 'fail') return decision;
	if (!isRateLimitWait(decision, context.status)) return decision;

	return { kind: 'fail', reason: `rate limited instead of waiting: ${decision.reason}` };
}

function isRateLimitWait(
	decision: RetryDecision,
	status: number
): decision is Extract<RetryDecision, { kind: 'pace' | 'retry' }> {
	if (decision.kind === 'pace') return true;

	return decision.kind === 'retry' && RATE_LIMIT_STATUSES.has(status);
}

const RATE_LIMIT_STATUSES = new Set([403, 429]);

function classify({
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
		return { kind: 'pace', waitMs: snapshot.retryAfterMs, reason: `Retry-After on ${resource}` };
	}

	if (isExhausted(snapshot) && (status === 403 || status === 429)) {
		return {
			kind: 'pace',
			waitMs: waitUntilReset(snapshot.resetAt, now),
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
