import { describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import type { RequestEvent as ListingsEvent } from './$types';
import type { RequestEvent as ListingEvent } from './[slug]/$types';
import type { Database } from '@yuki/db';
import type { ListingDetail, ListingSummary } from '$lib/server/listings.ts';

type SerializedVersion = Omit<ListingDetail['versions'][number], 'publishedAt'> & {
	publishedAt: string | null;
};

type SerializedDetail = Omit<ListingDetail, 'versions'> & { versions: SerializedVersion[] };

const { getFeaturedListings, getListingBySlug, getListingsPage } = vi.hoisted(() => ({
	getFeaturedListings: vi.fn(),
	getListingBySlug: vi.fn(),
	getListingsPage: vi.fn()
}));

vi.mock('$lib/server/listings.ts', () => ({
	getFeaturedListings,
	getListingBySlug,
	getListingsPage
}));

const { GET: listings } = await import('./+server.ts');
const { GET: listingBySlug } = await import('./[slug]/+server.ts');

const db = {} as Database;

function summary(overrides: Partial<ListingSummary> = {}): ListingSummary {
	return {
		id: '3f1b5c4e-0000-4000-8000-000000000001',
		githubRepoId: 4242,
		slug: 'acme-tools',
		title: 'Acme Tools',
		author: 'acme',
		description: 'A toolbox',
		iconUrl: 'https://example.com/icon.png',
		bannerUrl: 'https://example.com/banner.png',
		stars: 128,
		...overrides
	};
}

function detail(): ListingDetail {
	return {
		...summary(),
		authorUrl: 'https://github.com/acme',
		repositoryUrl: 'https://github.com/acme/tools',
		homepageUrl: null,
		license: 'MIT',
		isArchived: false,
		screenshots: [{ url: 'https://example.com/shot.png', alt: 'Home' }],
		versions: [
			{
				tag: 'v1.2.0',
				name: 'Release 1.2.0',
				downloadUrl: 'https://example.com/app.apk',
				assetName: 'app.apk',
				isPrerelease: false,
				publishedAt: new Date('2026-01-02T03:04:05.000Z')
			}
		]
	};
}

function listingsEvent(search: string) {
	return {
		locals: { db },
		url: new URL(`http://localhost/api/listings${search}`)
	} as unknown as ListingsEvent;
}

function slugEvent(slug: string) {
	return {
		locals: { db },
		params: { slug },
		url: new URL(`http://localhost/api/listings/${slug}`)
	} as unknown as ListingEvent;
}

describe('GET /api/listings', () => {
	it('returns the browse page for a request without the featured flag', async () => {
		getListingsPage.mockResolvedValue({ results: [summary()], hasMore: true });

		const response = await listings(listingsEvent('?sort=newest&order=asc&offset=24'));

		expect(getListingsPage).toHaveBeenCalledWith(db, {
			limit: 24,
			offset: 24,
			sort: 'newest',
			order: 'asc'
		});
		expect(getFeaturedListings).not.toHaveBeenCalled();
		await expect(response.json()).resolves.toEqual({ results: [summary()], hasMore: true });
	});

	it('returns the featured listings under the same envelope', async () => {
		getFeaturedListings.mockResolvedValue([summary({ slug: 'featured-app' })]);

		const response = await listings(listingsEvent('?featured=true'));

		expect(getFeaturedListings).toHaveBeenCalledWith(db, 10);
		await expect(response.json()).resolves.toEqual({
			results: [summary({ slug: 'featured-app' })],
			hasMore: false
		});
	});

	it('exposes the github repo id every summary is keyed by', async () => {
		getListingsPage.mockResolvedValue({
			results: [summary({ githubRepoId: 909 })],
			hasMore: false
		});

		const response = await listings(listingsEvent(''));
		const page = (await response.json()) as { results: ListingSummary[] };

		expect(page.results[0]?.githubRepoId).toBe(909);
	});
});

describe('GET /api/listings/[slug]', () => {
	it('returns the listing detail for a published slug', async () => {
		getListingBySlug.mockResolvedValue(detail());

		const response = await listingBySlug(slugEvent('acme-tools'));
		const body = (await response.json()) as SerializedDetail;

		expect(getListingBySlug).toHaveBeenCalledWith(db, 'acme-tools');
		expect(response.status).toBe(200);
		expect(body.githubRepoId).toBe(4242);
		expect(body.screenshots).toEqual([{ url: 'https://example.com/shot.png', alt: 'Home' }]);
		expect(body.versions[0]?.downloadUrl).toBe('https://example.com/app.apk');
		expect(body.versions[0]?.publishedAt).toBe('2026-01-02T03:04:05.000Z');
	});

	it('fails with a 404 when no listing matches the slug', async () => {
		getListingBySlug.mockResolvedValue(null);

		try {
			await listingBySlug(slugEvent('missing'));
			expect.unreachable('the handler should have failed');
		} catch (thrown) {
			expect(isHttpError(thrown)).toBe(true);
			if (isHttpError(thrown)) {
				expect(thrown.status).toBe(404);
				expect(thrown.body.message).toBe('Listing not found');
			}
		}
	});
});
