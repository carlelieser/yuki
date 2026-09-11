import { describe, expect, it, vi } from 'vitest';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';
import type { ListingSummary } from '$lib/server/listings.ts';

const { searchListingsTypeahead } = vi.hoisted(() => ({
	searchListingsTypeahead: vi.fn()
}));

vi.mock('$lib/server/listing-search.ts', () => ({ searchListingsTypeahead }));

const { GET: search } = await import('./+server.ts');

const db = {} as Database;

function summary(overrides: Partial<ListingSummary> = {}): ListingSummary {
	return {
		id: '3f1b5c4e-0000-4000-8000-000000000001',
		githubRepoId: 4242,
		slug: 'acme-tools',
		title: 'Acme Tools',
		author: 'acme',
		description: 'A toolbox',
		iconUrl: null,
		bannerUrl: null,
		category: null,
		stars: 128,
		ratingAverage: 4.6,
		ratingCount: 12,
		...overrides
	};
}

function searchEvent(search: string) {
	return {
		locals: { db },
		url: new URL(`http://localhost/api/search${search}`)
	} as unknown as RequestEvent;
}

describe('GET /api/search', () => {
	it('sorts the matches by the requested sort instead of relevance', async () => {
		searchListingsTypeahead.mockResolvedValue([summary()]);

		await search(searchEvent('?q=tools&sort=name&order=asc'));

		expect(searchListingsTypeahead).toHaveBeenCalledWith(
			db,
			'tools',
			expect.objectContaining({ sorting: { sort: 'name', order: 'asc' } })
		);
	});

	it('falls back to relevance when no sort is requested', async () => {
		searchListingsTypeahead.mockResolvedValue([]);

		await search(searchEvent('?q=tools'));

		expect(searchListingsTypeahead).toHaveBeenCalledWith(
			db,
			'tools',
			expect.objectContaining({ sorting: { sort: 'relevance', order: 'desc' } })
		);
	});

	it('honours a requested limit', async () => {
		searchListingsTypeahead.mockResolvedValue([]);

		await search(searchEvent('?q=tools&limit=24'));

		expect(searchListingsTypeahead).toHaveBeenCalledWith(
			db,
			'tools',
			expect.objectContaining({ limit: 24 })
		);
	});

	it('caps an oversized limit', async () => {
		searchListingsTypeahead.mockResolvedValue([]);

		await search(searchEvent('?q=tools&limit=5000'));

		expect(searchListingsTypeahead).toHaveBeenCalledWith(
			db,
			'tools',
			expect.objectContaining({ limit: 24 })
		);
	});

	it('passes a known category through to the query', async () => {
		searchListingsTypeahead.mockResolvedValue([]);

		await search(searchEvent('?q=tools&category=gaming'));

		expect(searchListingsTypeahead).toHaveBeenCalledWith(
			db,
			'tools',
			expect.objectContaining({ category: 'gaming' })
		);
	});

	it('returns an empty envelope without querying for a blank query', async () => {
		const response = await search(searchEvent('?q=%20%20'));

		expect(searchListingsTypeahead).not.toHaveBeenCalled();
		await expect(response.json()).resolves.toEqual({ results: [] });
	});

	it('returns the matches under a results envelope', async () => {
		searchListingsTypeahead.mockResolvedValue([summary({ slug: 'found' })]);

		const response = await search(searchEvent('?q=tools'));

		await expect(response.json()).resolves.toEqual({ results: [summary({ slug: 'found' })] });
	});
});
