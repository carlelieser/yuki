import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { getListingBySlug } from '$lib/server/listings.ts';

export const load: PageServerLoad = async ({ locals, params }) => {
	const listing = await getListingBySlug(locals.db, params.slug);
	if (listing === null) error(404, 'Listing not found');

	return { listing };
};
