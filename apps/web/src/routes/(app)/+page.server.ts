import type { PageServerLoad } from './$types';
import { getFeaturedListings, getListingsPage, getRecentListings } from '$lib/server/listings.ts';
import { BROWSE_PAGE_SIZE, readBrowseSorting } from '$lib/browse.ts';

const FEATURED_LIMIT = 12;
const RECENT_LIMIT = 8;

export const load: PageServerLoad = async ({ locals, url }) => {
	const { sort, order } = readBrowseSorting(url.searchParams);

	const [featured, recent, browse] = await Promise.all([
		getFeaturedListings(locals.db, FEATURED_LIMIT),
		getRecentListings(locals.db, RECENT_LIMIT),
		getListingsPage(locals.db, { limit: BROWSE_PAGE_SIZE, offset: 0, sort, order })
	]);

	return { featured, recent, browse, sort, order };
};
