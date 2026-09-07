export type RateLimitSnapshot = {
	remaining: number | null;
	resetAt: Date | null;
	retryAfterMs: number | null;
};

function parseInteger(value: string | null): number | null {
	if (value === null) return null;
	const parsed = Number.parseInt(value, 10);
	return Number.isFinite(parsed) ? parsed : null;
}

export function readRateLimit(headers: Headers): RateLimitSnapshot {
	const remaining = parseInteger(headers.get('x-ratelimit-remaining'));
	const resetSeconds = parseInteger(headers.get('x-ratelimit-reset'));
	const retryAfterSeconds = parseInteger(headers.get('retry-after'));

	return {
		remaining,
		resetAt: resetSeconds === null ? null : new Date(resetSeconds * 1000),
		retryAfterMs: retryAfterSeconds === null ? null : Math.max(0, retryAfterSeconds * 1000)
	};
}

export function waitUntilReset(resetAt: Date | null, now: Date = new Date()): number {
	if (resetAt === null) return 0;
	return Math.max(0, resetAt.getTime() - now.getTime());
}

export function isExhausted(snapshot: RateLimitSnapshot): boolean {
	return snapshot.remaining !== null && snapshot.remaining <= 0;
}
