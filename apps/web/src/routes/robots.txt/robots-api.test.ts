import { describe, expect, it, vi } from 'vitest';
import type { RequestEvent } from './$types';

vi.mock('$env/dynamic/public', () => ({ env: { PUBLIC_SITE_URL: 'https://yukistore.org' } }));

const { GET: robots } = await import('./+server.ts');

function robotsEvent() {
	const setHeaders = vi.fn();
	const event = { setHeaders } as unknown as RequestEvent;

	return { event, setHeaders };
}

describe('GET /robots.txt', () => {
	it('serves plain text', async () => {
		const { event } = robotsEvent();
		const response = await robots(event);

		expect(response.status).toBe(200);
		expect(response.headers.get('content-type')).toBe('text/plain');
	});

	it('points crawlers at an absolute sitemap url', async () => {
		const { event } = robotsEvent();
		const body = await (await robots(event)).text();

		expect(body).toContain('Sitemap: https://yukistore.org/sitemap.xml');
	});

	it('keeps crawlers out of search, api, and download redirects', async () => {
		const { event } = robotsEvent();
		const body = await (await robots(event)).text();

		expect(body).toContain('Disallow: /search');
		expect(body).toContain('Disallow: /api/');
		expect(body).toContain('Disallow: /listings/*/download/');
	});

	it('leaves listing pages crawlable', async () => {
		const { event } = robotsEvent();
		const body = await (await robots(event)).text();

		expect(body).not.toMatch(/Disallow: \/listings\/?$/m);
	});
});
