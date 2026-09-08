import { describe, expect, it } from 'vitest';
import { schema, type Database } from '@yuki/db';
import { getListingBySlug, summaryColumns } from './listings.ts';

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
