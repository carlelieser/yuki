import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { searchListingsTypeahead } from '$lib/server/listing-search.ts';
import { normalizeSearchQuery, readSearchLimit, readSearchSorting } from '$lib/search-query.ts';
import { readCategory } from '$lib/categories.ts';

export const GET: RequestHandler = async ({ locals, url }) => {
	const query = normalizeSearchQuery(url.searchParams.get('q'));
	if (query === '') return json({ results: [] });

	const results = await searchListingsTypeahead(locals.db, query, {
		limit: readSearchLimit(url.searchParams.get('limit')),
		sorting: readSearchSorting(url.searchParams),
		category: readCategory(url.searchParams.get('category'))
	});

	return json({ results });
};
