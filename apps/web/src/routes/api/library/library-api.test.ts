import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';
import type { SessionUser } from '@yuki/auth';

const { findPublishedListingId, getLibrary, recordDownload } = vi.hoisted(() => ({
	findPublishedListingId: vi.fn(),
	getLibrary: vi.fn(),
	recordDownload: vi.fn()
}));

vi.mock('$lib/server/library.ts', () => ({ findPublishedListingId, getLibrary }));
vi.mock('$lib/server/reviews.ts', () => ({ recordDownload }));

const { GET: readLibrary, POST: addToLibrary } = await import('./+server.ts');

const user = { id: 'user-1' } as SessionUser;
const db = {} as Database;

function event(body?: unknown, signedIn = true) {
	return {
		locals: { db, user: signedIn ? user : null },
		request: new Request('http://localhost/api/library', {
			method: body === undefined ? 'GET' : 'POST',
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
	findPublishedListingId.mockReset();
	getLibrary.mockReset();
	recordDownload.mockReset();
});

describe('GET /api/library', () => {
	it('returns only the signed-in user library', async () => {
		const entry = { slug: 'aurora-store', packageName: 'com.aurora.store' };
		getLibrary.mockResolvedValue([entry]);

		const response = await readLibrary(event());

		expect(getLibrary).toHaveBeenCalledWith(db, 'user-1');
		await expect(response.json()).resolves.toEqual({ results: [entry] });
	});

	it('refuses an anonymous request so a library cannot be read for others', async () => {
		await expectStatus(readLibrary, event(undefined, false), 401);
		expect(getLibrary).not.toHaveBeenCalled();
	});
});

describe('POST /api/library', () => {
	it('records the listing against the signed-in user', async () => {
		findPublishedListingId.mockResolvedValue('listing-1');

		const response = await addToLibrary(event({ slug: 'aurora-store', versionTag: 'v1.2.0' }));

		expect(recordDownload).toHaveBeenCalledWith(db, {
			listingId: 'listing-1',
			userId: 'user-1',
			versionTag: 'v1.2.0'
		});
		await expect(response.json()).resolves.toEqual({ slug: 'aurora-store' });
	});

	it('accepts a repeat call so syncing the same install twice is harmless', async () => {
		findPublishedListingId.mockResolvedValue('listing-1');

		await addToLibrary(event({ slug: 'aurora-store', versionTag: 'v1.2.0' }));
		await addToLibrary(event({ slug: 'aurora-store', versionTag: 'v1.2.0' }));

		expect(recordDownload).toHaveBeenCalledTimes(2);
	});

	it('treats a missing version tag as unknown rather than failing', async () => {
		findPublishedListingId.mockResolvedValue('listing-1');

		await addToLibrary(event({ slug: 'aurora-store' }));

		expect(recordDownload).toHaveBeenCalledWith(
			db,
			expect.objectContaining({ versionTag: null })
		);
	});

	it('refuses an anonymous request so a library cannot be written for others', async () => {
		await expectStatus(addToLibrary, event({ slug: 'aurora-store' }, false), 401);
		expect(recordDownload).not.toHaveBeenCalled();
	});

	it('reports an unknown slug instead of recording nothing silently', async () => {
		findPublishedListingId.mockResolvedValue(null);

		await expectStatus(addToLibrary, event({ slug: 'missing-app' }), 404);
		expect(recordDownload).not.toHaveBeenCalled();
	});

	it('rejects a body that is not shaped like a library entry', async () => {
		await expectStatus(addToLibrary, event('not json'), 400);
		await expectStatus(addToLibrary, event({ slug: 42 }), 400);
		await expectStatus(addToLibrary, event({ slug: 'aurora-store', versionTag: 7 }), 400);
		expect(recordDownload).not.toHaveBeenCalled();
	});
});
