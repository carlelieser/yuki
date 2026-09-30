import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Database } from '@yuki/db';
import type { ListingDetail } from './listings.ts';

const { getReleaseByTag, createGithubClient } = vi.hoisted(() => ({
	getReleaseByTag: vi.fn(),
	createGithubClient: vi.fn()
}));

vi.mock('@yuki/github', async (importOriginal) => ({
	...(await importOriginal<typeof import('@yuki/github')>()),
	createGithubClient
}));

const { GithubSkip } = await import('@yuki/github');
const { fetchReleaseAssets, ReleaseLookupFailed } = await import('./release-assets.ts');

type StoredAsset = { name: string; size: number; downloadUrl: string; isForeign: boolean };

function storing(assets: StoredAsset[]): Database {
	const chain = {
		from: () => chain,
		innerJoin: () => chain,
		where: () => Promise.resolve(assets)
	};

	return { select: () => chain } as unknown as Database;
}

function stored(name: string, isForeign = false): StoredAsset {
	return { name, size: 10, downloadUrl: `https://example.com/${name}`, isForeign };
}

const listing = {
	id: 'listing-1',
	slug: 'acme-tools',
	repositoryUrl: 'https://github.com/acme/tools'
} as ListingDetail;

beforeEach(() => {
	vi.stubEnv('GITHUB_TOKEN', 'token');
	getReleaseByTag.mockReset();
	createGithubClient.mockReturnValue({ getReleaseByTag });
});

afterEach(() => {
	vi.unstubAllEnvs();
});

describe('fetchReleaseAssets', () => {
	it('returns the assets of a release github knows', async () => {
		getReleaseByTag.mockResolvedValue({
			isModified: true,
			body: {
				assets: [{ name: 'app.apk', size: 5, browser_download_url: 'https://example.com/app.apk' }]
			},
			etag: null
		});

		await expect(fetchReleaseAssets(storing([]), listing, 'v1')).resolves.toEqual({
			kind: 'found',
			assets: [{ name: 'app.apk', size: 5, downloadUrl: 'https://example.com/app.apk' }]
		});
		expect(getReleaseByTag).toHaveBeenCalledWith('acme', 'tools', 'v1');
	});

	it('fails a rate limited lookup instead of holding the request open', async () => {
		getReleaseByTag.mockResolvedValue({ isModified: true, body: { assets: [] }, etag: null });

		await fetchReleaseAssets(storing([]), listing, 'v1');

		expect(createGithubClient).toHaveBeenCalledWith(
			'token',
			expect.anything(),
			expect.objectContaining({ onRateLimit: 'fail' })
		);
	});

	it('reports a release github does not have as missing', async () => {
		getReleaseByTag.mockRejectedValue(new GithubSkip('repos/acme/tools is unavailable (404)'));

		await expect(fetchReleaseAssets(storing([]), listing, 'v1')).resolves.toEqual({
			kind: 'missing'
		});
	});

	it('reports any other github failure as a failed lookup', async () => {
		getReleaseByTag.mockRejectedValue(new Error('returned 502 on 1 attempts'));

		await expect(fetchReleaseAssets(storing([]), listing, 'v1')).rejects.toBeInstanceOf(
			ReleaseLookupFailed
		);
	});

	it('refuses to run without a github token', async () => {
		vi.stubEnv('GITHUB_TOKEN', '');

		await expect(fetchReleaseAssets(storing([]), listing, 'v1')).rejects.toThrow('GITHUB_TOKEN');
		expect(getReleaseByTag).not.toHaveBeenCalled();
	});

	it('refuses a listing whose repository url names no repository', async () => {
		const broken = { ...listing, repositoryUrl: 'https://github.com/acme' } as ListingDetail;

		await expect(fetchReleaseAssets(storing([]), broken, 'v1')).rejects.toThrow('acme-tools');
	});

	it('serves a release its stored assets without asking github', async () => {
		const release = await fetchReleaseAssets(storing([stored('app-arm64-v8a.apk')]), listing, 'v1');

		expect(release).toEqual({
			kind: 'found',
			assets: [expect.objectContaining({ name: 'app-arm64-v8a.apk' })]
		});
		expect(getReleaseByTag).not.toHaveBeenCalled();
	});

	it('never offers a foreign asset of a stored release', async () => {
		const release = await fetchReleaseAssets(
			storing([stored('manager.apk'), stored('arcore.apk', true)]),
			listing,
			'v1'
		);

		expect(release.kind === 'found' && release.assets.map((asset) => asset.name)).toEqual([
			'manager.apk'
		]);
	});

	it('finds nothing to offer in a release whose stored assets are all foreign', async () => {
		const release = await fetchReleaseAssets(storing([stored('arcore.apk', true)]), listing, 'v1');

		expect(release).toEqual({ kind: 'found', assets: [] });
		expect(getReleaseByTag).not.toHaveBeenCalled();
	});
});
