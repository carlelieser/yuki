import { describe, expect, it, vi } from 'vitest';
import { QueryBuilder } from 'drizzle-orm/pg-core';
import { schema, type Database } from '@yuki/db';
import type { ListingCategory } from '$lib/categories.ts';
import type { RankedRow } from './listings.ts';

const { getRatingSummary } = vi.hoisted(() => ({ getRatingSummary: vi.fn() }));

vi.mock('./reviews.ts', () => ({ getRatingSummary }));

const { getListingBySlug, getListingsPage, groupIntoSections, summaryColumns } = await import(
	'./listings.ts'
);

function ratingOf(average: number, total: number) {
	return { average, total, distribution: [] };
}

function rowFor(overrides: Record<string, unknown> = {}) {
	return {
		id: '3f1b5c4e-0000-4000-8000-000000000001',
		githubRepoId: 4242,
		slug: 'acme-tools',
		title: 'Acme Tools',
		author: 'acme',
		authorUrl: 'https://github.com/acme',
		description: 'A toolbox',
		iconUrl: 'https://example.com/icon.png',
		bannerUrl: 'https://example.com/banner.png',
		stars: 128,
		repositoryUrl: 'https://github.com/acme/tools',
		homepageUrl: null,
		license: 'MIT',
		isArchived: false,
		screenshots: [{ url: 'https://example.com/shot.png', alt: 'Home', position: 0 }],
		versions: [
			{
				tag: 'v1.2.0',
				name: 'Release 1.2.0',
				downloadUrl: 'https://example.com/app.apk',
				assetName: 'app.apk',
				isPrerelease: false,
				publishedAt: new Date('2026-01-02T03:04:05.000Z')
			}
		],
		...overrides
	};
}

function databaseReturning(row: unknown) {
	return {
		query: { listings: { findFirst: async () => row } }
	} as unknown as Database;
}

function summarySelectSql(): string {
	return new QueryBuilder().select(summaryColumns).from(schema.listings).toSQL().sql;
}

describe('summaryColumns', () => {
	it('selects the github repo id clients key installs by', () => {
		expect(summaryColumns.githubRepoId).toBe(schema.listings.githubRepoId);
	});

	it('correlates the rating subqueries to the listing row being selected', () => {
		const rendered = summarySelectSql();

		expect(rendered).toContain('"listing_id" = "listings"."id"');
		expect(rendered).not.toContain('"listing_id" = "id"');
	});
});

describe('getListingBySlug', () => {
	it('maps a published listing onto the detail shape', async () => {
		getRatingSummary.mockResolvedValue(ratingOf(4.6, 12));

		const listing = await getListingBySlug(databaseReturning(rowFor()), 'acme-tools');

		expect(listing).toEqual({
			id: '3f1b5c4e-0000-4000-8000-000000000001',
			githubRepoId: 4242,
			slug: 'acme-tools',
			title: 'Acme Tools',
			author: 'acme',
			authorUrl: 'https://github.com/acme',
			description: 'A toolbox',
			iconUrl: 'https://example.com/icon.png',
			bannerUrl: 'https://example.com/banner.png',
			stars: 128,
			ratingAverage: 4.6,
			ratingCount: 12,
			repositoryUrl: 'https://github.com/acme/tools',
			homepageUrl: null,
			license: 'MIT',
			isArchived: false,
			screenshots: [{ url: 'https://example.com/shot.png', alt: 'Home' }],
			versions: [
				{
					tag: 'v1.2.0',
					name: 'Release 1.2.0',
					downloadUrl: 'https://example.com/app.apk',
					assetName: 'app.apk',
					isPrerelease: false,
					publishedAt: new Date('2026-01-02T03:04:05.000Z')
				}
			]
		});
	});

	it('leaves the average unset when a listing has no reviews', async () => {
		getRatingSummary.mockResolvedValue(ratingOf(0, 0));

		const listing = await getListingBySlug(databaseReturning(rowFor()), 'acme-tools');

		expect(listing?.ratingAverage).toBeNull();
		expect(listing?.ratingCount).toBe(0);
	});

	it('returns null when no published listing matches the slug', async () => {
		expect(await getListingBySlug(databaseReturning(undefined), 'missing')).toBeNull();
	});
});

function rankedRowFor(overrides: Partial<RankedRow> = {}): RankedRow {
	return {
		id: '3f1b5c4e-0000-4000-8000-000000000001',
		githubRepoId: 4242,
		slug: 'acme-tools',
		title: 'Acme Tools',
		author: 'acme',
		description: 'A toolbox',
		iconUrl: 'https://example.com/icon.png',
		bannerUrl: 'https://example.com/banner.png',
		stars: 128,
		category: 'gaming',
		ratingAverage: 4.6,
		ratingCount: 12,
		rank: 1,
		...overrides
	};
}

function rankedSection(category: ListingCategory, starCounts: number[]): RankedRow[] {
	return starCounts.map((stars, index) =>
		rankedRowFor({
			id: `${category}-${index}`,
			slug: `${category}-${index}`,
			category,
			stars,
			rank: index + 1
		})
	);
}

describe('groupIntoSections', () => {
	it('groups ranked rows into one section per category', () => {
		const sections = groupIntoSections(
			[...rankedSection('gaming', [90, 80]), ...rankedSection('media', [70])],
			3
		);

		expect(sections.map((section) => section.category)).toEqual(['gaming', 'media']);
		expect(sections[0]?.results).toHaveLength(2);
		expect(sections[1]?.results).toHaveLength(1);
	});

	it('keeps listings within a section ordered by stars descending', () => {
		const [section] = groupIntoSections(rankedSection('gaming', [300, 200, 100]), 3);

		expect(section?.results.map((listing) => listing.stars)).toEqual([300, 200, 100]);
	});

	it('keeps at most the requested number of listings per section', () => {
		const [section] = groupIntoSections(rankedSection('gaming', [90, 80, 70, 60, 50]), 3);

		expect(section?.results).toHaveLength(3);
		expect(section?.results.map((listing) => listing.stars)).toEqual([90, 80, 70]);
	});

	it('carries the rating onto each section result', () => {
		const [section] = groupIntoSections(rankedSection('gaming', [90]), 3);

		expect(section?.results[0]?.ratingAverage).toBe(4.6);
		expect(section?.results[0]?.ratingCount).toBe(12);
	});

	it('keeps an unrated listing without an average', () => {
		const [section] = groupIntoSections(
			[rankedRowFor({ category: 'gaming', ratingAverage: null, ratingCount: 0 })],
			3
		);

		expect(section?.results[0]?.ratingAverage).toBeNull();
		expect(section?.results[0]?.ratingCount).toBe(0);
	});

	it('excludes listings with no category', () => {
		const sections = groupIntoSections(
			[rankedRowFor({ category: null }), ...rankedSection('media', [70])],
			3
		);

		expect(sections.map((section) => section.category)).toEqual(['media']);
	});

	it('omits categories that have no listings', () => {
		const sections = groupIntoSections(rankedSection('gaming', [90]), 3);

		expect(sections).toHaveLength(1);
		expect(sections[0]?.category).toBe('gaming');
	});

	it('returns no sections when nothing is published', () => {
		expect(groupIntoSections([], 3)).toEqual([]);
	});

	it('drops summary fields that are not part of the public shape', () => {
		const [section] = groupIntoSections(rankedSection('gaming', [90]), 3);

		expect(section?.results[0]).not.toHaveProperty('rank');
	});
});

function pageDatabase(rows: unknown[]) {
	const captured: { where?: unknown } = {};
	const builder = {
		select: () => builder,
		from: () => builder,
		where: (condition: unknown) => {
			captured.where = condition;
			return builder;
		},
		orderBy: () => builder,
		limit: () => builder,
		offset: async () => rows
	};

	return { db: builder as unknown as Database, captured };
}

function renderedWhere(captured: { where?: unknown }): string {
	const rendered = new QueryBuilder()
		.select({ id: schema.listings.id })
		.from(schema.listings)
		.where(captured.where as never)
		.toSQL().sql;

	return rendered.slice(rendered.indexOf(' where '));
}

function summaryRows(count: number) {
	return Array.from({ length: count }, (_, index) => ({ slug: `listing-${index}` }));
}

describe('getListingsPage', () => {
	it('filters by author alongside the published check', async () => {
		const { db, captured } = pageDatabase([]);

		await getListingsPage(db, { limit: 24, offset: 0, author: 'acme' });

		const sql = renderedWhere(captured);
		expect(sql).toContain('"is_published"');
		expect(sql).toContain('"author"');
	});

	it('combines an author filter with a category filter', async () => {
		const { db, captured } = pageDatabase([]);

		await getListingsPage(db, { limit: 24, offset: 0, author: 'acme', category: 'gaming' });

		const sql = renderedWhere(captured);
		expect(sql).toContain('"author"');
		expect(sql).toContain('"category"');
	});

	it('leaves the author unfiltered when none is given', async () => {
		const { db, captured } = pageDatabase([]);

		await getListingsPage(db, { limit: 24, offset: 0 });

		expect(renderedWhere(captured)).not.toContain('"author"');
	});

	it('reports more pages when the lookahead row comes back', async () => {
		const { db } = pageDatabase(summaryRows(25));

		const page = await getListingsPage(db, { limit: 24, offset: 0, author: 'acme' });

		expect(page.results).toHaveLength(24);
		expect(page.hasMore).toBe(true);
	});

	it('reports no more pages on the last page of an author', async () => {
		const { db } = pageDatabase(summaryRows(5));

		const page = await getListingsPage(db, { limit: 24, offset: 0, author: 'acme' });

		expect(page.results).toHaveLength(5);
		expect(page.hasMore).toBe(false);
	});
});
