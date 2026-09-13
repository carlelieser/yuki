import { describe, expect, it, vi } from 'vitest';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';
import type { SitemapListing } from '$lib/server/sitemap-listings.ts';

const { getSitemapListings } = vi.hoisted(() => ({ getSitemapListings: vi.fn() }));

vi.mock('$lib/server/sitemap-listings.ts', () => ({ getSitemapListings }));
vi.mock('$env/static/public', () => ({ PUBLIC_SITE_URL: 'https://yukistore.org' }));

const { GET: sitemap } = await import('./+server.ts');

const db = {} as Database;

function listing(overrides: Partial<SitemapListing> = {}): SitemapListing {
	return { slug: 'acme-tools', repoPushedAt: new Date('2026-02-01T00:00:00Z'), ...overrides };
}

function sitemapEvent() {
	const setHeaders = vi.fn();
	const event = { locals: { db }, setHeaders } as unknown as RequestEvent;

	return { event, setHeaders };
}

describe('GET /sitemap.xml', () => {
	it('serves xml', async () => {
		getSitemapListings.mockResolvedValue([]);

		const { event } = sitemapEvent();
		const response = await sitemap(event);

		expect(response.status).toBe(200);
		expect(response.headers.get('content-type')).toBe('application/xml');
	});

	it('reads listings from the request database', async () => {
		getSitemapListings.mockResolvedValue([]);

		const { event } = sitemapEvent();
		await sitemap(event);

		expect(getSitemapListings).toHaveBeenCalledWith(db);
	});

	it('includes an absolute url for every published listing', async () => {
		getSitemapListings.mockResolvedValue([listing(), listing({ slug: 'widget' })]);

		const { event } = sitemapEvent();
		const body = await (await sitemap(event)).text();

		expect(body).toContain('<loc>https://yukistore.org/listings/acme-tools</loc>');
		expect(body).toContain('<loc>https://yukistore.org/listings/widget</loc>');
	});

	it('carries the repository push date as lastmod', async () => {
		getSitemapListings.mockResolvedValue([listing()]);

		const { event } = sitemapEvent();
		const body = await (await sitemap(event)).text();

		expect(body).toContain('<lastmod>2026-02-01</lastmod>');
	});

	it('still lists static routes when nothing is published', async () => {
		getSitemapListings.mockResolvedValue([]);

		const { event } = sitemapEvent();
		const body = await (await sitemap(event)).text();

		expect(body).toContain('<loc>https://yukistore.org/</loc>');
		expect(body).toContain('<loc>https://yukistore.org/browse</loc>');
	});

	it('excludes the search route from the sitemap', async () => {
		getSitemapListings.mockResolvedValue([]);

		const { event } = sitemapEvent();
		const body = await (await sitemap(event)).text();

		expect(body).not.toContain('/search');
	});

	it('sets a cache-control header', async () => {
		getSitemapListings.mockResolvedValue([]);

		const { event, setHeaders } = sitemapEvent();
		await sitemap(event);

		expect(setHeaders).toHaveBeenCalledWith({ 'cache-control': 'public, max-age=3600' });
	});
});
