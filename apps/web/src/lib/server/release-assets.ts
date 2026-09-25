import {
	availableArchitectures,
	createGithubClient,
	type Architecture,
	type GithubReleaseAsset
} from '@yuki/github';
import type { ListingDetail } from './listings.ts';

const MAX_ATTEMPTS = 1;

function sleep(ms: number): Promise<void> {
	return new Promise((resolve) => setTimeout(resolve, ms));
}

function repositoryPath(listing: ListingDetail): { owner: string; name: string } | null {
	const segments = new URL(listing.repositoryUrl).pathname.split('/').filter(Boolean);
	const [owner, name] = segments;
	if (!owner || !name) return null;

	return { owner, name };
}

export async function fetchReleaseAssets(
	listing: ListingDetail,
	tag: string
): Promise<GithubReleaseAsset[] | null> {
	const token = process.env.GITHUB_TOKEN;
	const path = repositoryPath(listing);
	if (!token || path === null) return null;

	const release = await createGithubClient(
		token,
		{ fetch, wait: sleep },
		{ maxAttempts: MAX_ATTEMPTS }
	)
		.getReleaseByTag(path.owner, path.name, tag)
		.catch(() => null);
	if (release === null || !release.isModified) return null;

	return release.body.assets;
}

export async function getReleaseArchitectures(
	listing: ListingDetail,
	tag: string
): Promise<Architecture[]> {
	const assets = await fetchReleaseAssets(listing, tag);
	if (assets === null) return [];

	return availableArchitectures(assets);
}
