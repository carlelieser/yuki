import type { RequestHandler } from './$types';
import { siteUrl } from '$lib/site.ts';

const CACHE_SECONDS = 86400;

const DISALLOWED = ['/search', '/api/', '/signout', '/listings/*/download/'];

export const GET: RequestHandler = ({ setHeaders }) => {
	const rules = DISALLOWED.map((path) => `Disallow: ${path}`).join('\n');
	const body = `User-agent: *\n${rules}\n\nSitemap: ${siteUrl('/sitemap.xml')}\n`;

	setHeaders({ 'cache-control': `public, max-age=${CACHE_SECONDS}` });

	return new Response(body, { headers: { 'content-type': 'text/plain' } });
};
