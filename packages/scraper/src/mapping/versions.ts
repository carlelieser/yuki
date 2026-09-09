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

export const ARCHITECTURES = ['arm64-v8a', 'armeabi-v7a', 'x86_64', 'x86'] as const;

export type Architecture = (typeof ARCHITECTURES)[number];

export function parseArchitecture(value: string | null): Architecture | null {
	const found = ARCHITECTURES.find((architecture) => architecture === value);
	return found ?? null;
}

function isApk(asset: GithubReleaseAsset): boolean {
	return asset.name.toLowerCase().endsWith('.apk');
}

function isDeprioritised(asset: GithubReleaseAsset): boolean {
	const lowered = asset.name.toLowerCase();
	return DEPRIORITISED.some((marker) => lowered.includes(marker));
}

function architectureOf(asset: GithubReleaseAsset): Architecture | null {
	const lowered = asset.name.toLowerCase();
	const matches = ARCHITECTURES.filter((architecture) => lowered.includes(architecture));
	if (matches.length === 0) return null;

	return matches.reduce((longest, architecture) =>
		architecture.length > longest.length ? architecture : longest
	);
}

function largestOf(assets: GithubReleaseAsset[]): GithubReleaseAsset {
	return assets.reduce((largest, asset) => (asset.size > largest.size ? asset : largest));
}

export function pickApkAsset(
	assets: GithubReleaseAsset[],
	architecture: Architecture | null = null
): GithubReleaseAsset | null {
	const apks = assets.filter(isApk);
	if (apks.length === 0) return null;

	const preferred = apks.filter((asset) => !isDeprioritised(asset));
	const pool = preferred.length > 0 ? preferred : apks;

	if (architecture === null) return largestOf(pool);

	const matching = pool.filter((asset) => architectureOf(asset) === architecture);
	if (matching.length > 0) return largestOf(matching);

	const universal = pool.filter((asset) => architectureOf(asset) === null);
	if (universal.length > 0) return largestOf(universal);

	return null;
}

export function hasDistributableApk(versions: MappedVersion[]): boolean {
	return versions.some((version) => version.downloadUrl !== null);
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
