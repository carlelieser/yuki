import { describe, expect, it, vi } from 'vitest';
import type { ListingDetail } from '../server/listings.ts';

vi.mock('$env/dynamic/public', () => ({ env: { PUBLIC_SITE_URL: 'https://yukistore.org' } }));

const { listingHeadline, listingImageUrl, listingMeta } = await import('./listing-meta.ts');

function listing(overrides: Partial<ListingDetail> = {}): ListingDetail {
	return {
		slug: 'wgtunnel-android',
		title: 'Android',
		author: 'wgtunnel',
		description: 'A WireGuard client with auto-tunneling and split tunneling support.',
		iconUrl: 'https://example.com/icon.png',
		bannerUrl: 'https://example.com/banner.png',
		category: 'networking',
		ratingAverage: null,
		ratingCount: 0,
		screenshots: [],
		versions: [],
		...overrides
	} as ListingDetail;
}

describe('listingHeadline', () => {
	it('names the author when the title alone is ambiguous', () => {
		expect(listingHeadline(listing())).toBe('Android by wgtunnel');
	});

	it('leaves a distinctive title alone', () => {
		expect(listingHeadline(listing({ title: 'SD Maid SE', author: 'd4rken-org' }))).toBe(
			'SD Maid SE'
		);
	});

	it('avoids repeating the author when it already matches the title', () => {
		expect(listingHeadline(listing({ title: 'Blocker', author: 'blocker' }))).toBe('Blocker');
	});

	it('treats a short multi-word title as distinctive', () => {
		expect(listingHeadline(listing({ title: 'Pi Hole', author: 'sanmerapps' }))).toBe('Pi Hole');
	});

	it('names the author for a bare repository name', () => {
		expect(listingHeadline(listing({ title: 'Pi', author: 'sanmerapps' }))).toBe(
			'Pi by sanmerapps'
		);
	});
});

describe('listingImageUrl', () => {
	it('prefers the icon', () => {
		expect(listingImageUrl(listing())).toBe('https://example.com/icon.png');
	});

	it('falls back to the banner when there is no icon', () => {
		expect(listingImageUrl(listing({ iconUrl: null }))).toBe('https://example.com/banner.png');
	});

	it('reports nothing when the listing has no art', () => {
		expect(listingImageUrl(listing({ iconUrl: null, bannerUrl: null }))).toBeNull();
	});
});

describe('listingMeta', () => {
	it('canonicalises to the listing slug', () => {
		expect(listingMeta(listing()).canonicalPath).toBe('/listings/wgtunnel-android');
	});

	it('disambiguates the page title', () => {
		expect(listingMeta(listing()).title).toBe('Android by wgtunnel · Yuki');
	});
});
