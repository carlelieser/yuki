import { describe, expect, it } from 'vitest';
import { associatedDownload } from './assets.ts';

function asset(name: string, overrides: { size?: number; isForeign?: boolean } = {}) {
	return {
		versionId: 'release-1',
		name,
		downloadUrl: `https://example.com/${name}`,
		size: overrides.size ?? 10,
		downloadCount: 3,
		isForeign: overrides.isForeign ?? false
	};
}

describe('associatedDownload', () => {
	it('picks the release apk from its own assets only', () => {
		const download = associatedDownload([
			asset('manager.apk', { size: 5 }),
			asset('arcore.apk', { size: 60, isForeign: true })
		]);

		expect(download).toEqual({
			downloadUrl: 'https://example.com/manager.apk',
			assetName: 'manager.apk',
			assetSize: 5,
			downloadCount: 3,
			isIgnored: false
		});
	});

	it('ignores a release whose apks are all foreign', () => {
		const download = associatedDownload([asset('arcore.apk', { isForeign: true })]);

		expect(download).toEqual({
			downloadUrl: null,
			assetName: null,
			assetSize: null,
			downloadCount: 0,
			isIgnored: true
		});
	});

	it('keeps the usual preference among own assets', () => {
		const download = associatedDownload([
			asset('app-debug.apk', { size: 90 }),
			asset('app-release.apk', { size: 20 })
		]);

		expect(download.assetName).toBe('app-release.apk');
	});
});
