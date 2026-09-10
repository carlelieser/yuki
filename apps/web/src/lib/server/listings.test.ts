import { describe, expect, it } from 'vitest';
import { schema, type Database } from '@yuki/db';
import type { ListingCategory } from '$lib/categories.ts';
import {
	getListingBySlug,
	groupIntoSections,
	summaryColumns,
	type RankedRow
} from './listings.ts';

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

describe('summaryColumns', () => {
	it('selects the github repo id clients key installs by', () => {
		expect(summaryColumns.githubRepoId).toBe(schema.listings.githubRepoId);
	});
});

describe('getListingBySlug', () => {
	it('maps a published listing onto the detail shape', async () => {
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
