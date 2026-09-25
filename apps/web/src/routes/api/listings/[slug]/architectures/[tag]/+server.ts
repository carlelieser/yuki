import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { getListingBySlug, type ListingDetail } from '$lib/server/listings.ts';
import { getReleaseArchitectures, ReleaseLookupFailed } from '$lib/server/release-assets.ts';

const CACHE_SECONDS = 600;

async function architecturesOf(listing: ListingDetail, tag: string) {
	try {
		return await getReleaseArchitectures(listing, tag);
	} catch (thrown) {
		if (!(thrown instanceof ReleaseLookupFailed)) throw thrown;
		error(502, 'GitHub unavailable');
	}
}

export const GET: RequestHandler = async ({ locals, params, setHeaders }) => {
	const listing = await getListingBySlug(locals.db, params.slug);
	if (listing === null) error(404, 'Listing not found');

	const version = listing.versions.find((entry) => entry.tag === params.tag);
	if (!version?.downloadUrl) error(404, 'Download not found');

	const release = await architecturesOf(listing, version.tag);
	if (release.kind === 'missing') error(404, 'Release not found');

	setHeaders({ 'cache-control': `public, max-age=${CACHE_SECONDS}` });

	return json({ architectures: release.architectures });
};
