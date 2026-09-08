import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { getListingBySlug } from '$lib/server/listings.ts';

export const GET: RequestHandler = async ({ locals, params }) => {
	const listing = await getListingBySlug(locals.db, params.slug);
	if (listing === null) error(404, 'Listing not found');

	return json(listing);
};
