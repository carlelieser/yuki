import { error, redirect } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import type { Database } from '@yuki/db';
import {
	architectureOfName,
	parseArchitecture,
	pickApkAsset,
	type Architecture
} from '@yuki/github';
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

function storedArchitecture(stored: StoredDownload): Architecture | null | undefined {
	return stored.assetName === null ? undefined : architectureOfName(stored.assetName);
}

function isUniversal(stored: StoredDownload): boolean {
	return storedArchitecture(stored) === null;
}

function fallbackFor(stored: StoredDownload): string {
	if (isUniversal(stored)) return stored.downloadUrl;

	error(502, 'GitHub unavailable');
}

function requestedArchitecture(url: URL): Architecture | null {
	const requested = url.searchParams.get('arch');
	if (requested === null) return null;

	const architecture = parseArchitecture(requested);
	if (architecture === null) error(400, 'Unknown architecture');

	return architecture;
}

async function resolveForArchitecture(
	listing: ListingDetail,
	stored: StoredDownload,
	architecture: Architecture
): Promise<string> {
	if (storedArchitecture(stored) === architecture) return stored.downloadUrl;

	try {
		const release = await fetchReleaseAssets(listing, stored.tag);
		if (release.kind === 'missing') error(404, 'Release not found');

		const asset = pickApkAsset(release.assets, architecture);
		if (asset === null) error(404, 'No build for this architecture');

		return asset.browser_download_url;
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
	const architecture = requestedArchitecture(url);
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
