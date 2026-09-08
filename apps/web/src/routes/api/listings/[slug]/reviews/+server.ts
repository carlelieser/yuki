import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { getListingBySlug } from '$lib/server/listings.ts';
import { getReviewsPage } from '$lib/server/reviews.ts';
import { readBrowseOffset, REVIEWS_PAGE_SIZE } from '$lib/browse.ts';

export const GET: RequestHandler = async ({ locals, params, url }) => {
	const listing = await getListingBySlug(locals.db, params.slug);
	if (listing === null) error(404, 'Listing not found');

	const offset = readBrowseOffset(url.searchParams.get('offset'));
	const page = await getReviewsPage(locals.db, listing.id, {
		limit: REVIEWS_PAGE_SIZE,
		offset
	});

	return json(page);
};
