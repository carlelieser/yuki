import type { RequestHandler } from './$types';
import { getSitemapListings } from '$lib/server/sitemap-listings.ts';
import { toSitemapEntries } from '$lib/sitemap-paths.ts';
import { toSitemapXml } from '$lib/sitemap-xml.ts';
import { siteUrl } from '$lib/site.ts';

const CACHE_SECONDS = 3600;

export const GET: RequestHandler = async ({ locals, setHeaders }) => {
	const listings = await getSitemapListings(locals.db);
	const xml = toSitemapXml(toSitemapEntries(listings), siteUrl);

	setHeaders({ 'cache-control': `public, max-age=${CACHE_SECONDS}` });

	return new Response(xml, { headers: { 'content-type': 'application/xml' } });
};
