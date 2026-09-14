import { describe, expect, it, vi } from 'vitest';
import type { ListingDetail } from '../server/listings.ts';

vi.mock('$env/dynamic/public', () => ({ env: { PUBLIC_SITE_URL: 'https://yukistore.org' } }));

const { toSoftwareApplicationSchema } = await import('./listing-schema.ts');

function listing(overrides: Partial<ListingDetail> = {}): ListingDetail {
	return {
		id: '3f1b5c4e-0000-4000-8000-000000000001',
		githubRepoId: 4242,
		slug: 'acme-tools',
		title: 'Acme Tools',
		author: 'acme',
		authorUrl: 'https://github.com/acme',
		description: 'Manage storage, clean junk files, and reclaim space on your device.',
		iconUrl: 'https://example.com/icon.png',
		bannerUrl: null,
		stars: 128,
		category: 'file_management',
		ratingAverage: null,
		ratingCount: 0,
		repositoryUrl: 'https://github.com/acme/tools',
		homepageUrl: null,
		license: 'MIT',
		isArchived: false,
		screenshots: [],
		versions: [],
		...overrides
	} as ListingDetail;
}

function parse(entry: ListingDetail): Record<string, unknown> {
	return JSON.parse(toSoftwareApplicationSchema(entry)) as Record<string, unknown>;
}

describe('toSoftwareApplicationSchema', () => {
	it('describes the listing as an android software application', () => {
		const schema = parse(listing());

		expect(schema['@type']).toBe('SoftwareApplication');
		expect(schema.operatingSystem).toBe('Android');
		expect(schema.name).toBe('Acme Tools');
	});

	it('points at the canonical listing url', () => {
		expect(parse(listing()).url).toBe('https://yukistore.org/listings/acme-tools');
	});

	it('omits the rating when the listing has no reviews', () => {
		expect(parse(listing())).not.toHaveProperty('aggregateRating');
	});

	it('includes the rating once reviews exist', () => {
		const schema = parse(listing({ ratingAverage: 4.6, ratingCount: 12 }));

		expect(schema.aggregateRating).toEqual({
			'@type': 'AggregateRating',
			ratingValue: 4.6,
			ratingCount: 12
		});
	});

	it('omits the category when the listing has none', () => {
		expect(parse(listing({ category: null }))).not.toHaveProperty('applicationCategory');
	});

	it('reports the newest release as the software version', () => {
		const versions = [
			{
				tag: 'v2.1.0',
				name: null,
				downloadUrl: null,
				assetName: null,
				isPrerelease: false,
				publishedAt: null
			}
		] as ListingDetail['versions'];

		expect(parse(listing({ versions })).softwareVersion).toBe('v2.1.0');
	});

	it('neutralises a script tag hidden in a scraped title', () => {
		const serialized = toSoftwareApplicationSchema(
			listing({ title: 'Acme</script><script>alert(1)</script>' })
		);

		expect(serialized).not.toContain('</script>');
		expect(serialized).not.toContain('<script>');
		expect(serialized).toContain('\\u003c');
	});

	it('keeps the escaped payload parseable and intact', () => {
		const title = 'Acme</script><img src=x onerror=alert(1)>';
		const schema = parse(listing({ title }));

		expect(schema.name).toBe(title);
	});

	it('escapes line separators that would break a script block', () => {
		const serialized = toSoftwareApplicationSchema(
			listing({ title: 'Acme Tools', description: null })
		);

		expect(serialized).not.toContain(' ');
	});
});
