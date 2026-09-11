import { describe, expect, it, vi } from 'vitest';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';
import type { CategorySection, ListingSummary } from '$lib/server/listings.ts';
import type { ListingCategory } from '$lib/categories.ts';

const { getCategorySections } = vi.hoisted(() => ({ getCategorySections: vi.fn() }));

vi.mock('$lib/server/listings.ts', () => ({ getCategorySections }));

const { GET: sections } = await import('./+server.ts');

const db = {} as Database;

function summary(overrides: Partial<ListingSummary> = {}): ListingSummary {
	return {
		id: '3f1b5c4e-0000-4000-8000-000000000001',
		githubRepoId: 4242,
		slug: 'acme-tools',
		title: 'Acme Tools',
		author: 'acme',
		description: 'A toolbox',
		iconUrl: 'https://example.com/icon.png',
		bannerUrl: 'https://example.com/banner.png',
		category: 'gaming',
		stars: 128,
		ratingAverage: 4.6,
		ratingCount: 12,
		...overrides
	};
}

function section(category: ListingCategory, results: ListingSummary[]): CategorySection {
	return { category, results };
}

function sectionsEvent(search: string) {
	const setHeaders = vi.fn();
	const event = {
		locals: { db },
		url: new URL(`http://localhost/api/feed${search}`),
		setHeaders
	} as unknown as RequestEvent;

	return { event, setHeaders };
}

describe('GET /api/feed', () => {
	it('asks for three listings per section by default', async () => {
		getCategorySections.mockResolvedValue([]);

		const { event } = sectionsEvent('');
		await sections(event);

		expect(getCategorySections).toHaveBeenCalledWith(db, 3);
	});

	it('passes an explicit limit through', async () => {
		getCategorySections.mockResolvedValue([]);

		const { event } = sectionsEvent('?limit=6');
		await sections(event);

		expect(getCategorySections).toHaveBeenCalledWith(db, 6);
	});

	it('clamps a limit that would dump the table', async () => {
		getCategorySections.mockResolvedValue([]);

		const { event } = sectionsEvent('?limit=100000');
		await sections(event);

		expect(getCategorySections).toHaveBeenCalledWith(db, 24);
	});

	it('falls back to the default for an unparseable limit', async () => {
		getCategorySections.mockResolvedValue([]);

		const { event } = sectionsEvent('?limit=abc');
		await sections(event);

		expect(getCategorySections).toHaveBeenCalledWith(db, 3);
	});

	it('returns an array of sections carrying their own category', async () => {
		getCategorySections.mockResolvedValue([
			section('gaming', [summary()]),
			section('media', [summary({ slug: 'player', category: 'media' })])
		]);

		const { event } = sectionsEvent('');
		const response = await sections(event);
		const body = (await response.json()) as { sections: CategorySection[] };

		expect(response.status).toBe(200);
		expect(Array.isArray(body.sections)).toBe(true);
		expect(body.sections.map((entry) => entry.category)).toEqual(['gaming', 'media']);
		expect(body.sections[0]?.results[0]?.githubRepoId).toBe(4242);
	});

	it('returns an empty array when nothing is published', async () => {
		getCategorySections.mockResolvedValue([]);

		const { event } = sectionsEvent('');
		const response = await sections(event);

		await expect(response.json()).resolves.toEqual({ sections: [] });
	});

	it('sets a cache-control header', async () => {
		getCategorySections.mockResolvedValue([]);

		const { event, setHeaders } = sectionsEvent('');
		await sections(event);

		expect(setHeaders).toHaveBeenCalledWith({ 'cache-control': 'public, max-age=300' });
	});
});
