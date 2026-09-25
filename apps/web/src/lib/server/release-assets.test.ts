import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ListingDetail } from './listings.ts';

const { getReleaseByTag } = vi.hoisted(() => ({ getReleaseByTag: vi.fn() }));

vi.mock('@yuki/github', async (importOriginal) => ({
	...(await importOriginal<typeof import('@yuki/github')>()),
	createGithubClient: () => ({ getReleaseByTag })
}));

const { GithubSkip } = await import('@yuki/github');
const { fetchReleaseAssets, ReleaseLookupFailed } = await import('./release-assets.ts');

const listing = {
	slug: 'acme-tools',
	repositoryUrl: 'https://github.com/acme/tools'
} as ListingDetail;

beforeEach(() => {
	vi.stubEnv('GITHUB_TOKEN', 'token');
	getReleaseByTag.mockReset();
});

afterEach(() => {
	vi.unstubAllEnvs();
});

describe('fetchReleaseAssets', () => {
	it('returns the assets of a release github knows', async () => {
		getReleaseByTag.mockResolvedValue({ isModified: true, body: { assets: [] }, etag: null });

		await expect(fetchReleaseAssets(listing, 'v1')).resolves.toEqual({ kind: 'found', assets: [] });
		expect(getReleaseByTag).toHaveBeenCalledWith('acme', 'tools', 'v1');
	});

	it('reports a release github does not have as missing', async () => {
		getReleaseByTag.mockRejectedValue(new GithubSkip('repos/acme/tools is unavailable (404)'));

		await expect(fetchReleaseAssets(listing, 'v1')).resolves.toEqual({ kind: 'missing' });
	});

	it('reports any other github failure as a failed lookup', async () => {
		getReleaseByTag.mockRejectedValue(new Error('returned 502 on 1 attempts'));

		await expect(fetchReleaseAssets(listing, 'v1')).rejects.toBeInstanceOf(ReleaseLookupFailed);
	});

	it('refuses to run without a github token', async () => {
		vi.stubEnv('GITHUB_TOKEN', '');

		await expect(fetchReleaseAssets(listing, 'v1')).rejects.toThrow('GITHUB_TOKEN');
		expect(getReleaseByTag).not.toHaveBeenCalled();
	});

	it('refuses a listing whose repository url names no repository', async () => {
		const broken = { ...listing, repositoryUrl: 'https://github.com/acme' } as ListingDetail;

		await expect(fetchReleaseAssets(broken, 'v1')).rejects.toThrow('acme-tools');
	});
});
