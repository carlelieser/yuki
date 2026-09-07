import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { searchListingsTypeahead } from '$lib/server/listing-search.ts';
import { normalizeSearchQuery } from '$lib/search-query.ts';

const TYPEAHEAD_LIMIT = 8;

export const GET: RequestHandler = async ({ locals, url }) => {
	const query = normalizeSearchQuery(url.searchParams.get('q'));
	if (query === '') return json({ results: [] });

	const results = await searchListingsTypeahead(locals.db, query, TYPEAHEAD_LIMIT);
	return json({ results });
};
