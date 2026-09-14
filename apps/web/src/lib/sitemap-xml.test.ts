import { describe, expect, it } from 'vitest';
import { toSitemapXml, type SitemapEntry } from './sitemap-xml.ts';

function absolute(path: string): string {
	return `https://yukistore.org${path}`;
}

function render(entries: SitemapEntry[]): string {
	return toSitemapXml(entries, absolute);
}

describe('toSitemapXml', () => {
	it('declares the sitemap namespace', () => {
		expect(render([])).toContain('xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"');
	});

	it('emits one url element per entry', () => {
		const xml = render([{ path: '/' }, { path: '/browse' }, { path: '/listings/acme' }]);

		expect(xml.match(/<url>/g)).toHaveLength(3);
	});

	it('resolves each path to an absolute location', () => {
		expect(render([{ path: '/listings/acme' }])).toContain(
			'<loc>https://yukistore.org/listings/acme</loc>'
		);
	});

	it('formats lastmod as a plain iso date', () => {
		const xml = render([{ path: '/listings/acme', lastmod: new Date('2026-02-01T13:45:12Z') }]);

		expect(xml).toContain('<lastmod>2026-02-01</lastmod>');
	});

	it('omits lastmod when the timestamp is null', () => {
		expect(render([{ path: '/listings/acme', lastmod: null }])).not.toContain('<lastmod>');
	});

	it('omits lastmod when the timestamp is absent', () => {
		expect(render([{ path: '/listings/acme' }])).not.toContain('<lastmod>');
	});

	it('escapes ampersands in query strings', () => {
		const xml = render([{ path: '/browse?sort=stars&order=desc' }]);

		expect(xml).toContain('/browse?sort=stars&amp;order=desc');
		expect(xml).not.toContain('stars&order');
	});

	it('escapes angle brackets and quotes that would break the document', () => {
		const xml = render([{ path: `/listings/<script>"'` }]);

		expect(xml).toContain('&lt;script&gt;&quot;&apos;');
		expect(xml).not.toContain('<script>');
	});
});
