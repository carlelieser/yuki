import { and, eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import {
	availableArchitectures,
	createGithubClient,
	GithubSkip,
	requireGithubToken,
	type Architecture,
	type GithubReleaseAsset,
	type RetryPolicy
} from '@yuki/github';
import type { ListingDetail } from './listings.ts';

const LOOKUP_POLICY: RetryPolicy = { maxAttempts: 1, onRateLimit: 'fail' };

export type ReleaseAsset = { name: string; size: number; downloadUrl: string };

export type ReleaseAssets = { kind: 'found'; assets: ReleaseAsset[] } | { kind: 'missing' };

export type ReleaseArchitectures =
	{ kind: 'found'; architectures: Architecture[] } | { kind: 'missing' };

export class ReleaseLookupFailed extends Error {
	constructor(listing: ListingDetail, tag: string, cause: unknown) {
		super(`Could not look up release ${tag} of ${listing.slug} on GitHub`, { cause });
	}
}

function sleep(ms: number): Promise<void> {
	return new Promise((resolve) => setTimeout(resolve, ms));
}

function repositoryPath(listing: ListingDetail): { owner: string; name: string } {
	const segments = new URL(listing.repositoryUrl).pathname.split('/').filter(Boolean);
	const [owner, name] = segments;
	if (!owner || !name) {
		throw new Error(`Listing ${listing.slug} has no GitHub repository in ${listing.repositoryUrl}`);
	}

	return { owner, name };
}

async function lookUpRelease(listing: ListingDetail, tag: string) {
	const path = repositoryPath(listing);
	const client = createGithubClient(requireGithubToken(), { fetch, wait: sleep }, LOOKUP_POLICY);

	try {
		return await client.getReleaseByTag(path.owner, path.name, tag);
	} catch (error) {
		if (error instanceof GithubSkip) return null;
		throw new ReleaseLookupFailed(listing, tag, error);
	}
}

export async function fetchReleaseAssets(
	db: Database,
	listing: ListingDetail,
	tag: string
): Promise<ReleaseAssets> {
	const stored = await storedReleaseAssets(db, listing.id, tag);
	if (stored.length > 0) {
		return { kind: 'found', assets: stored.filter((asset) => !asset.isForeign) };
	}

	const release = await lookUpRelease(listing, tag);
	if (release === null) return { kind: 'missing' };
	if (!release.isModified) {
		throw new Error(`GitHub answered 304 for release ${tag} of ${listing.slug} without an etag`);
	}

	return { kind: 'found', assets: release.body.assets.map(fromGithub) };
}

async function storedReleaseAssets(db: Database, listingId: string, tag: string) {
	const assets = schema.listingVersionAssets;
	const versions = schema.listingVersions;

	return db
		.select({
			name: assets.name,
			size: assets.size,
			downloadUrl: assets.downloadUrl,
			isForeign: assets.isForeign
		})
		.from(assets)
		.innerJoin(versions, eq(versions.id, assets.versionId))
		.where(and(eq(versions.listingId, listingId), eq(versions.tag, tag)));
}

function fromGithub(asset: GithubReleaseAsset): ReleaseAsset {
	return { name: asset.name, size: asset.size, downloadUrl: asset.browser_download_url };
}

export async function getReleaseArchitectures(
	db: Database,
	listing: ListingDetail,
	tag: string
): Promise<ReleaseArchitectures> {
	const release = await fetchReleaseAssets(db, listing, tag);
	if (release.kind === 'missing') return release;

	return { kind: 'found', architectures: availableArchitectures(release.assets) };
}
