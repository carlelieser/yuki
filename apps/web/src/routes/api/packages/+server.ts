import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { getPackageIndex } from '$lib/server/package-index.ts';

const CACHE_SECONDS = 3600;

export const GET: RequestHandler = async ({ locals, setHeaders }) => {
	const packages = await getPackageIndex(locals.db);

	setHeaders({ 'cache-control': `public, max-age=${CACHE_SECONDS}` });

	return json({ packages });
};
