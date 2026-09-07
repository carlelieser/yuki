import { describe, expect, it } from 'vitest';
import { mapReleases, pickApkAsset } from './versions.ts';
import type { GithubRelease, GithubReleaseAsset } from '../github/types.ts';

function asset(overrides: Partial<GithubReleaseAsset> = {}): GithubReleaseAsset {
	return {
		name: 'app-release.apk',
		browser_download_url: 'https://example.com/app-release.apk',
		size: 1000,
		download_count: 5,
		content_type: 'application/vnd.android.package-archive',
		...overrides
	};
}

function release(overrides: Partial<GithubRelease> = {}): GithubRelease {
	return {
		tag_name: 'v1.0.0',
		name: 'Release 1.0.0',
		body: 'Notes',
		draft: false,
		prerelease: false,
		published_at: '2026-01-01T00:00:00Z',
		assets: [asset()],
		...overrides
	};
}

describe('mapReleases', () => {
	it('drops drafts', () => {
		const mapped = mapReleases([
			release({ tag_name: 'v1' }),
			release({ tag_name: 'v2', draft: true })
		]);

		expect(mapped.map((version) => version.tag)).toEqual(['v1']);
	});

	it('keeps prereleases but flags them', () => {
		const mapped = mapReleases([release({ tag_name: 'v2-beta', prerelease: true })]);

		expect(mapped).toHaveLength(1);
		expect(mapped[0]?.isPrerelease).toBe(true);
	});

	it('maps the published date and leaves a missing one null', () => {
		const mapped = mapReleases([release(), release({ tag_name: 'v2', published_at: null })]);

		expect(mapped[0]?.publishedAt).toEqual(new Date('2026-01-01T00:00:00Z'));
		expect(mapped[1]?.publishedAt).toBeNull();
	});

	it('nulls an unparseable published_at instead of producing an Invalid Date', () => {
		const mapped = mapReleases([release({ published_at: 'not a date' })]);

		expect(mapped[0]?.publishedAt).toBeNull();
	});

	it('leaves download fields null when a release ships no apk', () => {
		const mapped = mapReleases([release({ assets: [asset({ name: 'sources.zip' })] })]);

		expect(mapped[0]).toMatchObject({
			downloadUrl: null,
			assetName: null,
			assetSize: null,
			downloadCount: 0
		});
	});
});

describe('pickApkAsset', () => {
	it('ignores non-apk assets', () => {
		expect(pickApkAsset([asset({ name: 'sources.zip' })])).toBeNull();
	});

	it('deprioritises debug builds when a release build exists', () => {
		const picked = pickApkAsset([
			asset({ name: 'app-debug.apk', size: 9000 }),
			asset({ name: 'app-release.apk', size: 100 })
		]);

		expect(picked?.name).toBe('app-release.apk');
	});

	it('still returns a debug build when it is the only apk', () => {
		const picked = pickApkAsset([asset({ name: 'app-debug.apk' })]);

		expect(picked?.name).toBe('app-debug.apk');
	});

	it('prefers the largest among equally ranked apks', () => {
		const picked = pickApkAsset([
			asset({ name: 'app-arm.apk', size: 100 }),
			asset({ name: 'app-universal.apk', size: 900 })
		]);

		expect(picked?.name).toBe('app-universal.apk');
	});
});
