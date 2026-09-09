import type { PageServerLoad } from './$types';
import { getListingsPage } from '$lib/server/listings.ts';
import { BROWSE_PAGE_SIZE, readBrowseOffset, readBrowseSorting } from '$lib/browse.ts';
import { CATEGORY_OPTIONS, readCategory } from '$lib/categories.ts';

export const load: PageServerLoad = async ({ locals, url }) => {
	const { sort, order } = readBrowseSorting(url.searchParams);
	const offset = readBrowseOffset(url.searchParams.get('offset'));
	const category = readCategory(url.searchParams.get('category'));

	const browse = await getListingsPage(locals.db, {
		limit: BROWSE_PAGE_SIZE + offset,
		offset: 0,
		sort,
		order,
		category
	});

	return { browse, sort, order, category, categories: CATEGORY_OPTIONS };
};
