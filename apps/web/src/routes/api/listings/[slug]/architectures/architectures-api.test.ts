import { describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import type { RequestEvent } from './[tag]/$types';
import type { Database } from '@yuki/db';
import type { ListingDetail } from '$lib/server/listings.ts';

const { getListingBySlug, getReleaseArchitectures } = vi.hoisted(() => ({
	getListingBySlug: vi.fn(),
	getReleaseArchitectures: vi.fn()
}));

vi.mock('$lib/server/listings.ts', () => ({ getListingBySlug }));
vi.mock('$lib/server/release-assets.ts', () => ({ getReleaseArchitectures }));

const { GET: architectures } = await import('./[tag]/+server.ts');

const db = {} as Database;

function detail(overrides: Partial<ListingDetail> = {}): ListingDetail {
	return {
		id: '3f1b5c4e-0000-4000-8000-000000000001',
		githubRepoId: 4242,
		slug: 'acme-tools',
		title: 'Acme Tools',
		author: 'acme',
		description: 'A toolbox',
		iconUrl: null,
		bannerUrl: null,
		category: null,
		stars: 128,
		authorUrl: 'https://github.com/acme',
		repositoryUrl: 'https://github.com/acme/tools',
		homepageUrl: null,
		license: 'MIT',
		isArchived: false,
		screenshots: [],
		versions: [
			{
				tag: 'v1.2.0',
				name: 'Release 1.2.0',
				downloadUrl: 'https://example.com/app.apk',
				assetName: 'app.apk',
				isPrerelease: false,
				publishedAt: new Date('2026-01-02T03:04:05.000Z')
			}
		],
		...overrides
	};
}

function event(slug: string, tag: string) {
	return {
		locals: { db },
		params: { slug, tag },
		setHeaders: vi.fn(),
		url: new URL(`http://localhost/api/listings/${slug}/architectures/${tag}`)
	} as unknown as RequestEvent;
}

async function expectNotFound(slug: string, tag: string, message: string): Promise<void> {
	try {
		await architectures(event(slug, tag));
		expect.unreachable('the handler should have failed');
	} catch (thrown) {
		expect(isHttpError(thrown)).toBe(true);
		if (isHttpError(thrown)) {
			expect(thrown.status).toBe(404);
			expect(thrown.body.message).toBe(message);
		}
	}
}

describe('GET /api/listings/[slug]/architectures/[tag]', () => {
	it('returns the architectures the release ships', async () => {
		getListingBySlug.mockResolvedValue(detail());
		getReleaseArchitectures.mockResolvedValue(['arm64-v8a', 'x86_64']);

		const response = await architectures(event('acme-tools', 'v1.2.0'));

		expect(getReleaseArchitectures).toHaveBeenCalledWith(expect.anything(), 'v1.2.0');
		await expect(response.json()).resolves.toEqual({
			architectures: ['arm64-v8a', 'x86_64']
		});
	});

	it('caches the response so reopening the menu skips github', async () => {
		getListingBySlug.mockResolvedValue(detail());
		getReleaseArchitectures.mockResolvedValue([]);

		const request = event('acme-tools', 'v1.2.0');
		await architectures(request);

		expect(request.setHeaders).toHaveBeenCalledWith({
			'cache-control': 'public, max-age=600'
		});
	});

	it('returns an empty list when the release ships no splits', async () => {
		getListingBySlug.mockResolvedValue(detail());
		getReleaseArchitectures.mockResolvedValue([]);

		const response = await architectures(event('acme-tools', 'v1.2.0'));

		await expect(response.json()).resolves.toEqual({ architectures: [] });
	});

	it('fails with a 404 when no listing matches the slug', async () => {
		getListingBySlug.mockResolvedValue(null);

		await expectNotFound('missing', 'v1.2.0', 'Listing not found');
	});

	it('fails with a 404 when the tag is not a release of this listing', async () => {
		getListingBySlug.mockResolvedValue(detail());

		await expectNotFound('acme-tools', 'v9.9.9', 'Download not found');
	});

	it('fails with a 404 when the release carries no downloadable asset', async () => {
		const version = { ...detail().versions[0]!, downloadUrl: null };
		getListingBySlug.mockResolvedValue(detail({ versions: [version] }));

		await expectNotFound('acme-tools', 'v1.2.0', 'Download not found');
	});
});
