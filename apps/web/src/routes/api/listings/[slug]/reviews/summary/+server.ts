import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { findPublishedListingId } from '$lib/server/library.ts';
import { getRatingSummary } from '$lib/server/reviews.ts';

export const GET: RequestHandler = async ({ locals, params }) => {
	const listingId = await findPublishedListingId(locals.db, params.slug);
	if (listingId === null) error(404, `Listing not found for slug "${params.slug}"`);

	return json(await getRatingSummary(locals.db, listingId));
};
