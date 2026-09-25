import { parseTimestamp } from './timestamp.ts';
import type { GithubRelease, GithubReleaseAsset } from './types.ts';

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

const ARCHITECTURE_ALIASES: Record<Architecture, readonly string[]> = {
	'arm64-v8a': ['arm64-v8a', 'arm64', 'aarch64'],
	'armeabi-v7a': ['armeabi-v7a', 'armeabi', 'armv7', 'arm32'],
	x86_64: ['x86_64', 'x86-64', 'x64'],
	x86: ['x86']
};

function hasToken(name: string, token: string): boolean {
	for (let index = name.indexOf(token); index !== -1; index = name.indexOf(token, index + 1)) {
		const before = name[index - 1];
		const after = name[index + token.length];
		if (!isTokenCharacter(before) && !isTokenCharacter(after)) return true;
	}

	return false;
}

function isTokenCharacter(character: string | undefined): boolean {
	return character !== undefined && /[a-z0-9]/.test(character);
}

function architectureOf(asset: GithubReleaseAsset): Architecture | null {
	return architectureOfName(asset.name);
}

export function architectureOfName(name: string): Architecture | null {
	const lowered = name.toLowerCase();
	const matches = ARCHITECTURES.filter((architecture) =>
		ARCHITECTURE_ALIASES[architecture].some((alias) => hasToken(lowered, alias))
	);
	if (matches.length === 0) return null;

	return matches.reduce((longest, architecture) =>
		architecture.length > longest.length ? architecture : longest
	);
}

function largestOf(assets: GithubReleaseAsset[]): GithubReleaseAsset {
	return assets.reduce((largest, asset) => (asset.size > largest.size ? asset : largest));
}

function distributableApks(assets: GithubReleaseAsset[]): GithubReleaseAsset[] {
	const apks = assets.filter(isApk);
	const preferred = apks.filter((asset) => !isDeprioritised(asset));
	return preferred.length > 0 ? preferred : apks;
}

export function availableArchitectures(assets: GithubReleaseAsset[]): Architecture[] {
	const found = new Set(
		distributableApks(assets)
			.map(architectureOf)
			.filter((architecture) => architecture !== null)
	);

	return ARCHITECTURES.filter((architecture) => found.has(architecture));
}

export function pickApkAsset(
	assets: GithubReleaseAsset[],
	architecture: Architecture | null = null
): GithubReleaseAsset | null {
	const pool = distributableApks(assets);
	if (pool.length === 0) return null;

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
