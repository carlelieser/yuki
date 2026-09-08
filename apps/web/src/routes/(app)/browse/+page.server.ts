import type { PageServerLoad } from './$types';
import { getListingsPage } from '$lib/server/listings.ts';
import { BROWSE_PAGE_SIZE, readBrowseOffset, readBrowseSorting } from '$lib/browse.ts';

export const load: PageServerLoad = async ({ locals, url }) => {
	const { sort, order } = readBrowseSorting(url.searchParams);
	const offset = readBrowseOffset(url.searchParams.get('offset'));

	const browse = await getListingsPage(locals.db, {
		limit: BROWSE_PAGE_SIZE + offset,
		offset: 0,
		sort,
		order
	});

	return { browse, sort, order };
};
