import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { getCategorySections } from '$lib/server/listings.ts';
import { readSectionLimit } from '$lib/browse.ts';

const CACHE_SECONDS = 300;

export const GET: RequestHandler = async ({ locals, url, setHeaders }) => {
	const limit = readSectionLimit(url.searchParams.get('limit'));
	const sections = await getCategorySections(locals.db, limit);

	setHeaders({ 'cache-control': `public, max-age=${CACHE_SECONDS}` });

	return json({ sections });
};
