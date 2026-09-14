import { CATEGORY_OPTIONS } from './categories.ts';
import type { SitemapEntry } from './sitemap-xml.ts';
import type { SitemapListing } from './server/sitemap-listings.ts';

const STATIC_PATHS = ['/', '/browse', '/terms', '/privacy'];

function categoryPaths(): SitemapEntry[] {
	return CATEGORY_OPTIONS.map((option) => ({ path: `/browse?category=${option.value}` }));
}

function listingPaths(listings: SitemapListing[]): SitemapEntry[] {
	return listings.map((listing) => ({
		path: `/listings/${listing.slug}`,
		lastmod: listing.repoPushedAt
	}));
}

export function toSitemapEntries(listings: SitemapListing[]): SitemapEntry[] {
	return [...STATIC_PATHS.map((path) => ({ path })), ...categoryPaths(), ...listingPaths(listings)];
}
