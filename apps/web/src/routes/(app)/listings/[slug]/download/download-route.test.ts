import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError, isRedirect } from '@sveltejs/kit';
import type { RequestEvent } from './[tag]/$types';
import type { Database } from '@yuki/db';
import type { GithubReleaseAsset } from '@yuki/github';
import type { ListingDetail } from '$lib/server/listings.ts';

const { getListingBySlug, fetchReleaseAssets, recordDownload, ReleaseLookupFailed } = vi.hoisted(
	() => ({
		getListingBySlug: vi.fn(),
		fetchReleaseAssets: vi.fn(),
		recordDownload: vi.fn(),
		ReleaseLookupFailed: class extends Error {}
	})
);

vi.mock('$lib/server/listings.ts', () => ({ getListingBySlug }));
vi.mock('$lib/server/release-assets.ts', () => ({ fetchReleaseAssets, ReleaseLookupFailed }));
vi.mock('$lib/server/reviews.ts', () => ({ recordDownload }));

const { GET: download } = await import('./[tag]/+server.ts');

const db = {} as Database;
const STORED_URL = 'https://github.com/acme/tools/releases/download/v1.2.0/app.apk';
const SPLIT_URL = 'https://github.com/acme/tools/releases/download/v1.2.0/app-arm64-v8a.apk';

function detail(assetName: string): ListingDetail {
	return {
		id: '3f1b5c4e-0000-4000-8000-000000000001',
		githubRepoId: 4242,
		slug: 'acme-tools',
		title: 'Acme Tools',
		author: 'acme',
		description: null,
		iconUrl: null,
		bannerUrl: null,
		category: null,
		stars: 1,
		ratingAverage: null,
		ratingCount: 0,
		authorUrl: 'https://github.com/acme',
		repositoryUrl: 'https://github.com/acme/tools',
		homepageUrl: null,
		license: null,
		isArchived: false,
		screenshots: [],
		versions: [
			{
				tag: 'v1.2.0',
				name: null,
				downloadUrl: STORED_URL,
				assetName,
				isPrerelease: false,
				publishedAt: null
			}
		]
	};
}

function asset(name: string, url: string): GithubReleaseAsset {
	return { name, browser_download_url: url, size: 100 } as GithubReleaseAsset;
}

function event(arch: string | null) {
	const query = arch === null ? '' : `?arch=${arch}`;
	return {
		locals: { db, user: null },
		params: { slug: 'acme-tools', tag: 'v1.2.0' },
		url: new URL(`http://localhost/listings/acme-tools/download/v1.2.0${query}`)
	} as unknown as RequestEvent;
}

async function outcomeOf(arch: string | null): Promise<{ status: number; target: string }> {
	try {
		await download(event(arch));
	} catch (thrown) {
		if (isRedirect(thrown)) return { status: thrown.status, target: thrown.location };
		if (isHttpError(thrown)) return { status: thrown.status, target: thrown.body.message };
		throw thrown;
	}

	throw new Error('the handler neither redirected nor failed');
}

beforeEach(() => {
	vi.resetAllMocks();
});

describe('GET /listings/[slug]/download/[tag]', () => {
	it('redirects to the stored apk when no architecture is asked for', async () => {
		getListingBySlug.mockResolvedValue(detail('app.apk'));

		expect(await outcomeOf(null)).toEqual({ status: 302, target: STORED_URL });
	});

	it('redirects to the split built for the requested architecture', async () => {
		getListingBySlug.mockResolvedValue(detail('app.apk'));
		fetchReleaseAssets.mockResolvedValue({
			kind: 'found',
			assets: [asset('app.apk', STORED_URL), asset('app-arm64-v8a.apk', SPLIT_URL)]
		});

		expect(await outcomeOf('arm64-v8a')).toEqual({ status: 302, target: SPLIT_URL });
	});

	it('falls back to a universal stored apk when github cannot be reached', async () => {
		getListingBySlug.mockResolvedValue(detail('app.apk'));
		fetchReleaseAssets.mockRejectedValue(new ReleaseLookupFailed('down'));

		expect(await outcomeOf('arm64-v8a')).toEqual({ status: 302, target: STORED_URL });
	});

	it('fails with a 502 when github is down and the stored apk is for another device', async () => {
		getListingBySlug.mockResolvedValue(detail('app-x86_64.apk'));
		fetchReleaseAssets.mockRejectedValue(new ReleaseLookupFailed('down'));

		expect(await outcomeOf('arm64-v8a')).toEqual({ status: 502, target: 'GitHub unavailable' });
	});

	it('fails with a 404 when github has no such release', async () => {
		getListingBySlug.mockResolvedValue(detail('app.apk'));
		fetchReleaseAssets.mockResolvedValue({ kind: 'missing' });

		expect(await outcomeOf('arm64-v8a')).toEqual({ status: 404, target: 'Release not found' });
	});

	it('fails with a 404 when the release has no build for the device', async () => {
		getListingBySlug.mockResolvedValue(detail('app-x86_64.apk'));
		fetchReleaseAssets.mockResolvedValue({
			kind: 'found',
			assets: [asset('app-x86_64.apk', STORED_URL)]
		});

		expect(await outcomeOf('arm64-v8a')).toEqual({
			status: 404,
			target: 'No build for this architecture'
		});
	});

	it('serves the universal apk when the release has no split for the device', async () => {
		getListingBySlug.mockResolvedValue(detail('app.apk'));
		fetchReleaseAssets.mockResolvedValue({
			kind: 'found',
			assets: [asset('app-x86_64.apk', SPLIT_URL), asset('app-universal.apk', STORED_URL)]
		});

		expect(await outcomeOf('arm64-v8a')).toEqual({ status: 302, target: STORED_URL });
	});
});
