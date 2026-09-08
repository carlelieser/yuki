import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { getListingsPage } from '$lib/server/listings.ts';
import { BROWSE_PAGE_SIZE, readBrowseOffset } from '$lib/browse.ts';

export const GET: RequestHandler = async ({ locals, url }) => {
	const offset = readBrowseOffset(url.searchParams.get('offset'));
	const page = await getListingsPage(locals.db, { limit: BROWSE_PAGE_SIZE, offset });

	return json(page);
};
