import type { PageServerLoad } from './$types';
import { getFeaturedListings, getRecentListings } from '$lib/server/listings.ts';

const FEATURED_LIMIT = 12;
const RECENT_LIMIT = 8;

export const load: PageServerLoad = async ({ locals }) => {
	const [featured, recent] = await Promise.all([
		getFeaturedListings(locals.db, FEATURED_LIMIT),
		getRecentListings(locals.db, RECENT_LIMIT)
	]);

	return { featured, recent };
};
