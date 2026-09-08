import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import type { Database } from '@yuki/db';
import { getFeaturedListings, getListingsPage, type ListingPage } from '$lib/server/listings.ts';
import {
	BROWSE_PAGE_SIZE,
	FEATURED_PAGE_SIZE,
	isFeaturedRequested,
	readBrowseOffset,
	readBrowseSorting
} from '$lib/browse.ts';

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
	const page = await getListingsPage(locals.db, { limit: BROWSE_PAGE_SIZE, offset, sort, order });

	return json(page);
};
