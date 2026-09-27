import type { PageServerLoad } from './$types';
import { getFeaturedListings, getListingsPage } from '$lib/server/listings.ts';
import { BROWSE_PAGE_SIZE, readBrowseSorting } from '$lib/browse.ts';
import { CATEGORY_OPTIONS } from '$lib/categories.ts';

const FEATURED_LIMIT = 12;
const ROW_LIMIT = 8;

export const load: PageServerLoad = async ({ locals, url }) => {
	const { sort, order } = readBrowseSorting(url.searchParams);

	const [featured, newest, updated, browse] = await Promise.all([
		getFeaturedListings(locals.db, FEATURED_LIMIT),
		getListingsPage(locals.db, { limit: ROW_LIMIT, offset: 0, sort: 'newest' }),
		getListingsPage(locals.db, { limit: ROW_LIMIT, offset: 0, sort: 'updated' }),
		getListingsPage(locals.db, { limit: BROWSE_PAGE_SIZE, offset: 0, sort, order })
	]);

	return {
		featured,
		newest: newest.results,
		updated: updated.results,
		browse,
		sort,
		order,
		categories: CATEGORY_OPTIONS
	};
};
