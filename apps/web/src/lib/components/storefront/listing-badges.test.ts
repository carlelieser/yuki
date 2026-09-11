import { describe, expect, it } from 'vitest';
import { badgesFor } from './listing-badges.ts';
import type { ListingSummary } from '$lib/server/listings.ts';

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
		category: 'gaming',
		stars: 128,
		ratingAverage: 4.6,
		ratingCount: 12,
		...overrides
	};
}

describe('badgesFor', () => {
	it('orders the badges category, stars, rating then author', () => {
		expect(badgesFor(summary()).map((badge) => badge.kind)).toEqual([
			'category',
			'stars',
			'rating',
			'author'
		]);
	});

	it('labels the category with its display name', () => {
		const [badge] = badgesFor(summary({ category: 'privacy_security' }));

		expect(badge).toEqual({ kind: 'category', label: 'Privacy', category: 'privacy_security' });
	});

	it('omits the category badge when a listing has none', () => {
		expect(badgesFor(summary({ category: null })).map((badge) => badge.kind)).toEqual([
			'stars',
			'rating',
			'author'
		]);
	});

	it('omits the rating badge when a listing has no reviews', () => {
		expect(badgesFor(summary({ ratingAverage: null, ratingCount: 0 })).map((b) => b.kind)).toEqual([
			'category',
			'stars',
			'author'
		]);
	});

	it('keeps the author badge for a listing with neither category nor rating', () => {
		const badges = badgesFor(summary({ category: null, ratingAverage: null, ratingCount: 0 }));

		expect(badges.map((badge) => badge.kind)).toEqual(['stars', 'author']);
	});
});
