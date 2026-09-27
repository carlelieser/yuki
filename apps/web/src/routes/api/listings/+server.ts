import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import type { Database } from '@yuki/db';
import { getFeaturedListings, getListingsPage, type ListingPage } from '$lib/server/listings.ts';
import {
	FEATURED_PAGE_SIZE,
	isFeaturedRequested,
	readAuthor,
	readBrowseLimit,
	readBrowseOffset,
	readBrowseSorting
} from '$lib/browse.ts';
import { readCategory } from '$lib/categories.ts';

async function featuredPage(db: Database): Promise<ListingPage> {
	const results = await getFeaturedListings(db, FEATURED_PAGE_SIZE);
	return { results, hasMore: false };
}

export const GET: RequestHandler = async ({ locals, url }) => {
	if (isFeaturedRequested(url.searchParams.get('featured'))) {
		return json(await featuredPage(locals.db));
	}

	const offset = readBrowseOffset(url.searchParams.get('offset'));
	const { sort, order } = readBrowseSorting(url.searchParams);
	const category = readCategory(url.searchParams.get('category'));
	const author = readAuthor(url.searchParams.get('author'));
	const page = await getListingsPage(locals.db, {
		limit: readBrowseLimit(url.searchParams.get('limit')),
		offset,
		sort,
		order,
		category,
		author
	});

	return json(page);
};
