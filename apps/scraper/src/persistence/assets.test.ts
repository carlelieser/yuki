import { describe, expect, it } from 'vitest';
import { associatedDownload } from './assets.ts';

type AssetOverrides = { size?: number; isForeign?: boolean; packageName?: string };

function asset(name: string, overrides: AssetOverrides = {}) {
	return {
		versionId: 'release-1',
		name,
		downloadUrl: `https://example.com/${name}`,
		size: overrides.size ?? 10,
		downloadCount: 3,
		isForeign: overrides.isForeign ?? false,
		packageName: overrides.packageName ?? 'com.acme.app',
		signer: 'acme',
		publishedAt: null,
		isPrerelease: false
	};
}

describe('associatedDownload', () => {
	it('picks the release apk from its own assets only', () => {
		const download = associatedDownload(
			[asset('manager.apk', { size: 5 }), asset('arcore.apk', { size: 60, isForeign: true })],
			null
		);

		expect(download).toEqual({
			downloadUrl: 'https://example.com/manager.apk',
			assetName: 'manager.apk',
			assetSize: 5,
			downloadCount: 3,
			isIgnored: false
		});
	});

	it('ignores a release whose apks are all foreign', () => {
		const download = associatedDownload([asset('arcore.apk', { isForeign: true })], null);

		expect(download).toEqual({
			downloadUrl: null,
			assetName: null,
			assetSize: null,
			downloadCount: 0,
			isIgnored: true
		});
	});

	it('keeps the usual preference among own assets', () => {
		const download = associatedDownload(
			[asset('app-debug.apk', { size: 90 }), asset('app-release.apk', { size: 20 })],
			null
		);

		expect(download.assetName).toBe('app-release.apk');
	});

	it("offers the listing's main app over a larger bundled one", () => {
		const download = associatedDownload(
			[
				asset('tidal-stock.apk', { size: 78, packageName: 'com.aspiro.tidal' }),
				asset('rl-manager.apk', { size: 8, packageName: 'com.meowarex.rlmobile' })
			],
			'com.meowarex.rlmobile'
		);

		expect(download.assetName).toBe('rl-manager.apk');
	});

	it("falls back to the release's own apks when it lacks the main app", () => {
		const download = associatedDownload(
			[asset('companion.apk', { packageName: 'com.acme.companion' })],
			'com.acme.app'
		);

		expect(download.assetName).toBe('companion.apk');
	});
});
