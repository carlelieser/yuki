import type { PageServerLoad } from './$types';
import { searchListingsPage } from '$lib/server/listing-search.ts';
import { normalizeSearchQuery, readOffset, readSearchSorting } from '$lib/search-query.ts';

const PAGE_SIZE = 24;

export const load: PageServerLoad = async ({ locals, url }) => {
	const query = normalizeSearchQuery(url.searchParams.get('q'));
	const offset = readOffset(url.searchParams.get('offset'));
	const sorting = readSearchSorting(url.searchParams);

	if (query === '') {
		return { query, offset: 0, sorting, results: [], total: 0, hasMore: false };
	}

	const page = await searchListingsPage(locals.db, query, {
		limit: PAGE_SIZE + offset,
		offset: 0,
		sorting
	});

	return { query, offset, sorting, ...page };
};
