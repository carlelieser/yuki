import { describe, expect, it } from 'vitest';
import {
	architectureOfName,
	availableArchitectures,
	hasDistributableApk,
	mapReleases,
	parseArchitecture,
	pickApkAsset
} from './versions.ts';
import type { GithubRelease, GithubReleaseAsset } from './types.ts';

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

	it('picks the asset matching the requested architecture over a larger one', () => {
		const picked = pickApkAsset(
			[
				asset({ name: 'app-arm64-v8a.apk', size: 100 }),
				asset({ name: 'app-x86_64.apk', size: 900 })
			],
			'arm64-v8a'
		);

		expect(picked?.name).toBe('app-arm64-v8a.apk');
	});

	it('does not treat armeabi-v7a as a match for arm64-v8a', () => {
		const picked = pickApkAsset([asset({ name: 'app-armeabi-v7a.apk' })], 'arm64-v8a');

		expect(picked).toBeNull();
	});

	it('falls back to a universal build when no split matches', () => {
		const picked = pickApkAsset(
			[
				asset({ name: 'app-armeabi-v7a.apk', size: 100 }),
				asset({ name: 'app-release.apk', size: 900 })
			],
			'x86_64'
		);

		expect(picked?.name).toBe('app-release.apk');
	});

	it('returns null when only non-matching splits exist', () => {
		const picked = pickApkAsset(
			[asset({ name: 'app-x86.apk' }), asset({ name: 'app-x86_64.apk' })],
			'arm64-v8a'
		);

		expect(picked).toBeNull();
	});

	it('keeps deprioritising debug builds when an architecture is requested', () => {
		const picked = pickApkAsset(
			[
				asset({ name: 'app-arm64-v8a-debug.apk', size: 900 }),
				asset({ name: 'app-arm64-v8a-release.apk', size: 100 })
			],
			'arm64-v8a'
		);

		expect(picked?.name).toBe('app-arm64-v8a-release.apk');
	});
});

describe('availableArchitectures', () => {
	it('is empty when a release ships only a universal build', () => {
		expect(availableArchitectures([asset({ name: 'app-release.apk' })])).toEqual([]);
	});

	it('is empty when a release ships no apk at all', () => {
		expect(availableArchitectures([asset({ name: 'sources.zip' })])).toEqual([]);
	});

	it('lists split architectures in a canonical order', () => {
		const found = availableArchitectures([
			asset({ name: 'app-x86.apk' }),
			asset({ name: 'app-arm64-v8a.apk' }),
			asset({ name: 'app-armeabi-v7a.apk' })
		]);

		expect(found).toEqual(['arm64-v8a', 'armeabi-v7a', 'x86']);
	});

	it('collapses repeated architectures to one entry', () => {
		const found = availableArchitectures([
			asset({ name: 'app-arm64-v8a.apk', size: 100 }),
			asset({ name: 'app-arm64-v8a-signed.apk', size: 200 })
		]);

		expect(found).toEqual(['arm64-v8a']);
	});

	it('does not offer an architecture that only a debug build covers', () => {
		const found = availableArchitectures([
			asset({ name: 'app-x86-debug.apk' }),
			asset({ name: 'app-arm64-v8a-release.apk' })
		]);

		expect(found).toEqual(['arm64-v8a']);
	});

	it('offers a debug-only architecture when the release ships nothing else', () => {
		expect(availableArchitectures([asset({ name: 'app-x86-debug.apk' })])).toEqual(['x86']);
	});

	it('ignores a universal build sitting alongside splits', () => {
		const found = availableArchitectures([
			asset({ name: 'app-release.apk' }),
			asset({ name: 'app-arm64-v8a.apk' })
		]);

		expect(found).toEqual(['arm64-v8a']);
	});
});

describe('architecture naming', () => {
	it('recognises the short arm spellings releases actually use', () => {
		const found = availableArchitectures([
			asset({ name: 'wgtunnel-standalone-v5.7.2-arm64.apk' }),
			asset({ name: 'wgtunnel-standalone-v5.7.2-armv7.apk' })
		]);

		expect(found).toEqual(['arm64-v8a', 'armeabi-v7a']);
	});

	it('recognises aarch64 as arm64', () => {
		expect(availableArchitectures([asset({ name: 'app-aarch64.apk' })])).toEqual(['arm64-v8a']);
	});

	it('does not read x86 out of an x86_64 asset', () => {
		expect(availableArchitectures([asset({ name: 'app-x86_64.apk' })])).toEqual(['x86_64']);
	});

	it('does not read an architecture out of a version number', () => {
		expect(availableArchitectures([asset({ name: 'app-v1.8664.apk' })])).toEqual([]);
	});

	it('does not treat arm64 inside a longer word as a match', () => {
		expect(availableArchitectures([asset({ name: 'apparm64x.apk' })])).toEqual([]);
	});

	it('routes a short-spelled asset to the requested architecture', () => {
		const picked = pickApkAsset(
			[
				asset({ name: 'wgtunnel-standalone-v5.7.2-arm64.apk', size: 100 }),
				asset({ name: 'wgtunnel-standalone-v5.7.2-armv7.apk', size: 900 })
			],
			'arm64-v8a'
		);

		expect(picked?.name).toBe('wgtunnel-standalone-v5.7.2-arm64.apk');
	});

	it('keeps a universal build out of the architecture list', () => {
		const found = availableArchitectures([
			asset({ name: 'wgtunnel-standalone-v5.7.2.apk' }),
			asset({ name: 'wgtunnel-standalone-v5.7.2-arm64.apk' })
		]);

		expect(found).toEqual(['arm64-v8a']);
	});
});

describe('architectureOfName', () => {
	it('reads the architecture a split apk was built for', () => {
		expect(architectureOfName('app-arm64-v8a-release.apk')).toBe('arm64-v8a');
		expect(architectureOfName('app-x86_64-release.apk')).toBe('x86_64');
	});

	it('reads no architecture from a universal apk', () => {
		expect(architectureOfName('app-universal-release.apk')).toBeNull();
		expect(architectureOfName('DPI-Zoom-v1.1.0.apk')).toBeNull();
	});
});

describe('parseArchitecture', () => {
	it('accepts a known abi', () => {
		expect(parseArchitecture('arm64-v8a')).toBe('arm64-v8a');
	});

	it('rejects anything else', () => {
		expect(parseArchitecture('sparc')).toBeNull();
		expect(parseArchitecture(null)).toBeNull();
	});
});

describe('hasDistributableApk', () => {
	it('is false when there are no releases at all', () => {
		expect(hasDistributableApk([])).toBe(false);
	});

	it('is false when no release carries an apk', () => {
		const versions = mapReleases([
			release({ assets: [asset({ name: 'sources.zip', size: 10 })] }),
			release({ tag_name: 'v2', assets: [] })
		]);

		expect(hasDistributableApk(versions)).toBe(false);
	});

	it('is true when any release carries an apk', () => {
		const versions = mapReleases([
			release({ assets: [asset({ name: 'sources.zip', size: 10 })] }),
			release({ tag_name: 'v2', assets: [asset({ name: 'app-release.apk', size: 20 })] })
		]);

		expect(hasDistributableApk(versions)).toBe(true);
	});
});
