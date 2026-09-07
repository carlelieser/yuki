import { describe, expect, it } from 'vitest';
import { MAX_ATTEMPTS, SECONDARY_LIMIT_WAIT_MS, decideRetry } from './backoff.ts';

function headers(entries: Record<string, string> = {}): Headers {
	return new Headers(entries);
}

describe('decideRetry', () => {
	it('treats a 404 on code search as throttling worth retrying', () => {
		const decision = decideRetry({
			status: 404,
			headers: headers(),
			attempt: 1,
			isCodeSearch: true,
			resource: 'search/code'
		});

		expect(decision.kind).toBe('retry');
	});

	it('treats a 404 on a repository path as terminal and names the resource', () => {
		const decision = decideRetry({
			status: 404,
			headers: headers(),
			attempt: 1,
			isCodeSearch: false,
			resource: 'repos/acme/gone'
		});

		expect(decision).toEqual({
			kind: 'skip',
			reason: 'repos/acme/gone is unavailable (404)'
		});
	});

	it('lets Retry-After win over the secondary limit default', () => {
		const decision = decideRetry({
			status: 403,
			headers: headers({ 'retry-after': '5' }),
			attempt: 1,
			isCodeSearch: false,
			resource: 'repos/acme/app'
		});

		expect(decision).toMatchObject({ kind: 'retry', waitMs: 5000 });
	});

	it('waits for the reset when the primary quota is exhausted', () => {
		const now = new Date('2026-01-01T00:00:00Z');
		const resetAt = Math.floor(new Date('2026-01-01T00:10:00Z').getTime() / 1000);

		const decision = decideRetry({
			status: 403,
			headers: headers({ 'x-ratelimit-remaining': '0', 'x-ratelimit-reset': String(resetAt) }),
			attempt: 1,
			isCodeSearch: false,
			resource: 'repos/acme/app',
			now
		});

		expect(decision).toMatchObject({ kind: 'retry', waitMs: 600_000 });
	});

	it('backs off at least a minute on a secondary limit without headers', () => {
		const decision = decideRetry({
			status: 429,
			headers: headers(),
			attempt: 1,
			isCodeSearch: false,
			resource: 'repos/acme/app'
		});

		expect(decision).toMatchObject({ kind: 'retry', waitMs: SECONDARY_LIMIT_WAIT_MS });
	});

	it('retries 5xx with exponential waits then fails once attempts run out', () => {
		const first = decideRetry({
			status: 502,
			headers: headers(),
			attempt: 1,
			isCodeSearch: false,
			resource: 'repos/acme/app'
		});
		const last = decideRetry({
			status: 502,
			headers: headers(),
			attempt: MAX_ATTEMPTS,
			isCodeSearch: false,
			resource: 'repos/acme/app'
		});

		expect(first).toMatchObject({ kind: 'retry', waitMs: 2000 });
		expect(last.kind).toBe('fail');
	});

	it('stops retrying code-search 404s once attempts run out', () => {
		const decision = decideRetry({
			status: 404,
			headers: headers(),
			attempt: MAX_ATTEMPTS,
			isCodeSearch: true,
			resource: 'search/code'
		});

		expect(decision.kind).toBe('fail');
	});

	it('passes through success and 304', () => {
		const ok = decideRetry({
			status: 200,
			headers: headers(),
			attempt: 1,
			isCodeSearch: false,
			resource: 'repos/acme/app'
		});
		const notModified = decideRetry({
			status: 304,
			headers: headers(),
			attempt: 1,
			isCodeSearch: false,
			resource: 'repos/acme/app'
		});

		expect(ok.kind).toBe('succeed');
		expect(notModified.kind).toBe('succeed');
	});

	it('skips repos that are gone or blocked', () => {
		for (const status of [409, 451]) {
			const decision = decideRetry({
				status,
				headers: headers(),
				attempt: 1,
				isCodeSearch: false,
				resource: 'repos/acme/app'
			});
			expect(decision.kind).toBe('skip');
		}
	});
});
