import { describe, expect, it } from 'vitest';
import { toSitemapEntries } from './sitemap-paths.ts';
import { CATEGORY_OPTIONS } from './categories.ts';
import type { SitemapListing } from './server/sitemap-listings.ts';

function listing(slug: string, repoPushedAt: Date | null = null): SitemapListing {
	return { slug, repoPushedAt };
}

describe('toSitemapEntries', () => {
	it('includes the home, browse, and legal routes', () => {
		const paths = toSitemapEntries([]).map((entry) => entry.path);

		expect(paths).toContain('/');
		expect(paths).toContain('/browse');
		expect(paths).toContain('/terms');
		expect(paths).toContain('/privacy');
	});

	it('includes a browse url for every category', () => {
		const paths = toSitemapEntries([]).map((entry) => entry.path);

		for (const option of CATEGORY_OPTIONS) {
			expect(paths).toContain(`/browse?category=${option.value}`);
		}
	});

	it('includes every listing with its push date', () => {
		const pushed = new Date('2026-03-04T00:00:00Z');
		const entries = toSitemapEntries([listing('acme', pushed)]);

		expect(entries).toContainEqual({ path: '/listings/acme', lastmod: pushed });
	});

	it('keeps a listing that has never been pushed', () => {
		const entries = toSitemapEntries([listing('acme')]);

		expect(entries).toContainEqual({ path: '/listings/acme', lastmod: null });
	});

	it('omits search and auth routes', () => {
		const paths = toSitemapEntries([listing('acme')]).map((entry) => entry.path);

		expect(paths).not.toContain('/search');
		expect(paths).not.toContain('/signin');
		expect(paths).not.toContain('/signup');
	});

	it('counts static, category, and listing urls together', () => {
		const entries = toSitemapEntries([listing('one'), listing('two')]);

		expect(entries).toHaveLength(4 + CATEGORY_OPTIONS.length + 2);
	});
});
