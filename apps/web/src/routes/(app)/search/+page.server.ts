import type { PageServerLoad } from './$types';
import { searchListingsPage } from '$lib/server/listing-search.ts';
import { normalizeSearchQuery, readOffset, readSearchSorting } from '$lib/search-query.ts';
import { CATEGORY_OPTIONS, readCategory } from '$lib/categories.ts';

const PAGE_SIZE = 24;

export const load: PageServerLoad = async ({ locals, url }) => {
	const query = normalizeSearchQuery(url.searchParams.get('q'));
	const offset = readOffset(url.searchParams.get('offset'));
	const sorting = readSearchSorting(url.searchParams);
	const category = readCategory(url.searchParams.get('category'));

	if (query === '') {
		return {
			query,
			offset: 0,
			sorting,
			category,
			categories: CATEGORY_OPTIONS,
			results: [],
			total: 0,
			hasMore: false
		};
	}

	const page = await searchListingsPage(locals.db, query, {
		limit: PAGE_SIZE + offset,
		offset: 0,
		sorting,
		category
	});

	return { query, offset, sorting, category, categories: CATEGORY_OPTIONS, ...page };
};
