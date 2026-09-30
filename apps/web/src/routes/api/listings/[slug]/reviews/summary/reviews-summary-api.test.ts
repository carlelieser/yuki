import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';

const { findPublishedListingId, getRatingSummary } = vi.hoisted(() => ({
	findPublishedListingId: vi.fn(),
	getRatingSummary: vi.fn()
}));

vi.mock('$lib/server/library.ts', () => ({ findPublishedListingId }));
vi.mock('$lib/server/reviews.ts', () => ({ getRatingSummary }));

const { GET: readSummary } = await import('./+server.ts');

const db = {} as Database;

function event(slug: string) {
	return { locals: { db, user: null }, params: { slug } } as unknown as RequestEvent;
}

beforeEach(() => {
	findPublishedListingId.mockReset();
	getRatingSummary.mockReset();
});

describe('GET /api/listings/[slug]/reviews/summary', () => {
	it('returns the rating summary of the listing to anyone', async () => {
		const summary = { average: 4.5, total: 2, distribution: [{ rating: 5, count: 1 }] };
		findPublishedListingId.mockResolvedValue('listing-1');
		getRatingSummary.mockResolvedValue(summary);

		const response = await readSummary(event('aurora-store'));

		expect(getRatingSummary).toHaveBeenCalledWith(db, 'listing-1');
		await expect(response.json()).resolves.toEqual(summary);
	});

	it('reports an unknown slug', async () => {
		findPublishedListingId.mockResolvedValue(null);

		try {
			await readSummary(event('missing-app'));
			expect.unreachable('the handler should have failed');
		} catch (thrown) {
			expect(isHttpError(thrown) && thrown.status).toBe(404);
		}
		expect(getRatingSummary).not.toHaveBeenCalled();
	});
});
