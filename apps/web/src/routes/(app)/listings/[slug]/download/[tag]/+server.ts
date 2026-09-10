import { error, redirect } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import type { Database } from '@yuki/db';
import { createGithubClient, parseArchitecture, pickApkAsset } from '@yuki/github';
import { getListingBySlug, type ListingDetail } from '$lib/server/listings.ts';
import { recordDownload } from '$lib/server/reviews.ts';

async function recordQuietly(
	db: Database,
	input: { listingId: string; userId: string; versionTag: string | null }
): Promise<void> {
	await recordDownload(db, input).catch(() => undefined);
}

function repositoryPath(listing: ListingDetail): { owner: string; name: string } | null {
	const segments = new URL(listing.repositoryUrl).pathname.split('/').filter(Boolean);
	const [owner, name] = segments;
	if (!owner || !name) return null;

	return { owner, name };
}

async function resolveForArchitecture(
	listing: ListingDetail,
	tag: string,
	architecture: string
): Promise<string | null> {
	const token = process.env.GITHUB_TOKEN;
	const path = repositoryPath(listing);
	if (!token || path === null) return null;

	const release = await createGithubClient(token)
		.getReleaseByTag(path.owner, path.name, tag)
		.catch(() => null);
	if (release === null || !release.isModified) return null;

	return (
		pickApkAsset(release.body.assets, parseArchitecture(architecture))?.browser_download_url ?? null
	);
}

export const GET: RequestHandler = async ({ locals, params, url }) => {
	const listing = await getListingBySlug(locals.db, params.slug);
	if (listing === null) error(404, 'Listing not found');

	const version = listing.versions.find((entry) => entry.tag === params.tag);
	if (!version?.downloadUrl) error(404, 'Download not found');

	const architecture = url.searchParams.get('arch');
	const resolved = architecture
		? await resolveForArchitecture(listing, version.tag, architecture)
		: null;

	if (locals.user) {
		await recordQuietly(locals.db, {
			listingId: listing.id,
			userId: locals.user.id,
			versionTag: version.tag
		});
	}

	redirect(302, resolved ?? version.downloadUrl);
};
