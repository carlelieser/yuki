import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';
import type { SessionUser } from '@yuki/auth';

const mocks = vi.hoisted(() => ({
	findPublishedListingId: vi.fn(),
	getUserReview: vi.fn(),
	hasDownloadedListing: vi.fn(),
	upsertReview: vi.fn(),
	deleteReview: vi.fn()
}));

vi.mock('$lib/server/library.ts', () => ({
	findPublishedListingId: mocks.findPublishedListingId
}));
vi.mock('$lib/server/reviews.ts', () => ({
	getUserReview: mocks.getUserReview,
	hasDownloadedListing: mocks.hasDownloadedListing,
	upsertReview: mocks.upsertReview,
	deleteReview: mocks.deleteReview
}));

const { GET: readOwn, PUT: saveOwn, DELETE: deleteOwn } = await import('./+server.ts');

const user = { id: 'user-1' } as SessionUser;
const db = {} as Database;
const review = {
	id: 'review-1',
	rating: 4,
	body: 'Solid',
	createdAt: '2026-01-01T00:00:00.000Z',
	author: { id: 'user-1', name: 'Ada', image: null }
};

type EventInput = { body?: unknown; isSignedIn?: boolean };

function event({ body, isSignedIn = true }: EventInput = {}) {
	return {
		locals: { db, user: isSignedIn ? user : null },
		params: { slug: 'aurora-store' },
		request: new Request('http://localhost/api/listings/aurora-store/reviews/mine', {
			method: body === undefined ? 'GET' : 'PUT',
			body: body === undefined || typeof body === 'string' ? body : JSON.stringify(body)
		})
	} as unknown as RequestEvent;
}

async function expectStatus(
	handler: (request: RequestEvent) => unknown,
	request: RequestEvent,
	status: number
): Promise<void> {
	try {
		await handler(request);
		expect.unreachable('the handler should have failed');
	} catch (thrown) {
		expect(isHttpError(thrown)).toBe(true);
		if (isHttpError(thrown)) expect(thrown.status).toBe(status);
	}
}

beforeEach(() => {
	Object.values(mocks).forEach((mock) => mock.mockReset());
	mocks.findPublishedListingId.mockResolvedValue('listing-1');
	mocks.hasDownloadedListing.mockResolvedValue(true);
	mocks.getUserReview.mockResolvedValue(review);
});

describe('GET /api/listings/[slug]/reviews/mine', () => {
	it('returns the signed-in user review and whether they may review', async () => {
		const response = await readOwn(event());

		expect(mocks.getUserReview).toHaveBeenCalledWith(db, 'listing-1', 'user-1');
		await expect(response.json()).resolves.toEqual({ review, canReview: true });
	});

	it('reports no review for a user who has not written one', async () => {
		mocks.getUserReview.mockResolvedValue(null);
		mocks.hasDownloadedListing.mockResolvedValue(false);

		const response = await readOwn(event());

		await expect(response.json()).resolves.toEqual({ review: null, canReview: false });
	});

	it('refuses an anonymous request', async () => {
		await expectStatus(readOwn, event({ isSignedIn: false }), 401);
	});

	it('reports an unknown slug', async () => {
		mocks.findPublishedListingId.mockResolvedValue(null);

		await expectStatus(readOwn, event(), 404);
	});
});

describe('PUT /api/listings/[slug]/reviews/mine', () => {
	it('saves the review against the signed-in user and returns it', async () => {
		const response = await saveOwn(event({ body: { rating: 4, body: '  Solid  ' } }));

		expect(mocks.upsertReview).toHaveBeenCalledWith(db, {
			listingId: 'listing-1',
			userId: 'user-1',
			rating: 4,
			body: 'Solid'
		});
		await expect(response.json()).resolves.toEqual({ review });
	});

	it('stores a blank body as no body', async () => {
		await saveOwn(event({ body: { rating: 5, body: '   ' } }));

		expect(mocks.upsertReview).toHaveBeenCalledWith(db, expect.objectContaining({ body: null }));
	});

	it('refuses a user who has not downloaded the app', async () => {
		mocks.hasDownloadedListing.mockResolvedValue(false);

		await expectStatus(saveOwn, event({ body: { rating: 4 } }), 403);
		expect(mocks.upsertReview).not.toHaveBeenCalled();
	});

	it('rejects a rating outside one to five and an overlong body', async () => {
		await expectStatus(saveOwn, event({ body: { rating: 0 } }), 400);
		await expectStatus(saveOwn, event({ body: { rating: 6 } }), 400);
		await expectStatus(saveOwn, event({ body: { rating: 3, body: 'a'.repeat(2001) } }), 400);
		await expectStatus(saveOwn, event({ body: 'not json' }), 400);
		expect(mocks.upsertReview).not.toHaveBeenCalled();
	});

	it('refuses an anonymous request', async () => {
		await expectStatus(saveOwn, event({ body: { rating: 4 }, isSignedIn: false }), 401);
		expect(mocks.upsertReview).not.toHaveBeenCalled();
	});

	it('reports an unknown slug', async () => {
		mocks.findPublishedListingId.mockResolvedValue(null);

		await expectStatus(saveOwn, event({ body: { rating: 4 } }), 404);
		expect(mocks.upsertReview).not.toHaveBeenCalled();
	});
});

describe('DELETE /api/listings/[slug]/reviews/mine', () => {
	it('deletes only the signed-in user review', async () => {
		const response = await deleteOwn(event());

		expect(mocks.deleteReview).toHaveBeenCalledWith(db, 'listing-1', 'user-1');
		expect(response.status).toBe(204);
	});

	it('refuses an anonymous request', async () => {
		await expectStatus(deleteOwn, event({ isSignedIn: false }), 401);
		expect(mocks.deleteReview).not.toHaveBeenCalled();
	});
});
