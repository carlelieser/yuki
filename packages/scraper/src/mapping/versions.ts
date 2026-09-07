import { parseTimestamp } from './listing.ts';
import type { GithubRelease, GithubReleaseAsset } from '../github/types.ts';

export type MappedVersion = {
	tag: string;
	name: string | null;
	notes: string | null;
	downloadUrl: string | null;
	assetName: string | null;
	assetSize: number | null;
	downloadCount: number;
	isPrerelease: boolean;
	publishedAt: Date | null;
};

const DEPRIORITISED = ['debug', 'unsigned', 'test'];

function isApk(asset: GithubReleaseAsset): boolean {
	return asset.name.toLowerCase().endsWith('.apk');
}

function isDeprioritised(asset: GithubReleaseAsset): boolean {
	const lowered = asset.name.toLowerCase();
	return DEPRIORITISED.some((marker) => lowered.includes(marker));
}

export function pickApkAsset(assets: GithubReleaseAsset[]): GithubReleaseAsset | null {
	const apks = assets.filter(isApk);
	if (apks.length === 0) return null;

	const preferred = apks.filter((asset) => !isDeprioritised(asset));
	const pool = preferred.length > 0 ? preferred : apks;

	return pool.reduce((largest, asset) => (asset.size > largest.size ? asset : largest));
}

export function mapReleases(releases: GithubRelease[]): MappedVersion[] {
	return releases
		.filter((release) => !release.draft)
		.map((release) => {
			const asset = pickApkAsset(release.assets);

			return {
				tag: release.tag_name,
				name: release.name,
				notes: release.body,
				downloadUrl: asset?.browser_download_url ?? null,
				assetName: asset?.name ?? null,
				assetSize: asset?.size ?? null,
				downloadCount: asset?.download_count ?? 0,
				isPrerelease: release.prerelease,
				publishedAt: parseTimestamp(release.published_at)
			};
		});
}
