import { error, redirect } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import type { Database } from '@yuki/db';
import { architectureOfName, parseArchitecture, pickApkAsset } from '@yuki/github';
import { getListingBySlug, type ListingDetail } from '$lib/server/listings.ts';
import { fetchReleaseAssets, ReleaseLookupFailed } from '$lib/server/release-assets.ts';
import { recordDownload } from '$lib/server/reviews.ts';

type StoredDownload = { tag: string; downloadUrl: string; assetName: string | null };

async function recordQuietly(
	db: Database,
	input: { listingId: string; userId: string; versionTag: string | null }
): Promise<void> {
	await recordDownload(db, input).catch(() => undefined);
}

function isUniversal(stored: StoredDownload): boolean {
	return stored.assetName !== null && architectureOfName(stored.assetName) === null;
}

function fallbackFor(stored: StoredDownload): string {
	if (isUniversal(stored)) return stored.downloadUrl;

	error(502, 'GitHub unavailable');
}

async function resolveForArchitecture(
	listing: ListingDetail,
	stored: StoredDownload,
	architecture: string
): Promise<string> {
	try {
		const release = await fetchReleaseAssets(listing, stored.tag);
		if (release.kind === 'missing') error(404, 'Release not found');

		const asset = pickApkAsset(release.assets, parseArchitecture(architecture));
		return asset?.browser_download_url ?? stored.downloadUrl;
	} catch (thrown) {
		if (!(thrown instanceof ReleaseLookupFailed)) throw thrown;
		return fallbackFor(stored);
	}
}

export const GET: RequestHandler = async ({ locals, params, url }) => {
	const listing = await getListingBySlug(locals.db, params.slug);
	if (listing === null) error(404, 'Listing not found');

	const version = listing.versions.find((entry) => entry.tag === params.tag);
	if (!version?.downloadUrl) error(404, 'Download not found');

	const stored = {
		tag: version.tag,
		downloadUrl: version.downloadUrl,
		assetName: version.assetName
	};
	const architecture = url.searchParams.get('arch');
	const resolved = architecture
		? await resolveForArchitecture(listing, stored, architecture)
		: stored.downloadUrl;

	if (locals.user) {
		await recordQuietly(locals.db, {
			listingId: listing.id,
			userId: locals.user.id,
			versionTag: version.tag
		});
	}

	redirect(302, resolved);
};
