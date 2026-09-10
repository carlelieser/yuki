import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { getListingBySlug } from '$lib/server/listings.ts';
import { getReleaseArchitectures } from '$lib/server/release-assets.ts';

const CACHE_SECONDS = 600;

export const GET: RequestHandler = async ({ locals, params, setHeaders }) => {
	const listing = await getListingBySlug(locals.db, params.slug);
	if (listing === null) error(404, 'Listing not found');

	const version = listing.versions.find((entry) => entry.tag === params.tag);
	if (!version?.downloadUrl) error(404, 'Download not found');

	const architectures = await getReleaseArchitectures(listing, version.tag);

	setHeaders({ 'cache-control': `public, max-age=${CACHE_SECONDS}` });

	return json({ architectures });
};
