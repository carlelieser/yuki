import { describe, expect, it } from 'vitest';
import { mapReleases } from './versions.ts';
import type { GithubRelease, GithubReleaseAsset } from './types.ts';

function asset(name: string, size: number): GithubReleaseAsset {
	return {
		name,
		browser_download_url: `https://example.com/${name}`,
		size,
		download_count: 2,
		content_type: 'application/vnd.android.package-archive'
	};
}

function release(assets: GithubReleaseAsset[]): GithubRelease {
	return {
		tag_name: 'v1.0.0',
		name: null,
		body: null,
		draft: false,
		prerelease: false,
		published_at: '2026-01-01T00:00:00Z',
		assets
	};
}

describe('release assets', () => {
	it('carries every apk asset of a release and nothing else', () => {
		const [mapped] = mapReleases([
			release([asset('app-arm64-v8a.apk', 30), asset('sources.zip', 5), asset('arcore.APK', 90)])
		]);

		expect(mapped?.assets).toEqual([
			{
				name: 'app-arm64-v8a.apk',
				downloadUrl: 'https://example.com/app-arm64-v8a.apk',
				size: 30,
				downloadCount: 2
			},
			{
				name: 'arcore.APK',
				downloadUrl: 'https://example.com/arcore.APK',
				size: 90,
				downloadCount: 2
			}
		]);
	});
});
