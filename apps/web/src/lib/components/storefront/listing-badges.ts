import { categoryLabel, type ListingCategory } from '$lib/categories.ts';
import type { ListingSummary } from '$lib/server/listings.ts';

export type BadgeSpec =
	| { kind: 'category'; label: string; category: ListingCategory }
	| { kind: 'stars'; stars: number }
	| { kind: 'rating'; average: number }
	| { kind: 'author'; label: string };

export function badgesFor(entry: ListingSummary): BadgeSpec[] {
	const specs: BadgeSpec[] = [];

	if (entry.category !== null) {
		specs.push({
			kind: 'category',
			label: categoryLabel(entry.category),
			category: entry.category
		});
	}

	specs.push({ kind: 'stars', stars: entry.stars });

	if (entry.ratingAverage !== null) {
		specs.push({ kind: 'rating', average: entry.ratingAverage });
	}

	specs.push({ kind: 'author', label: entry.author });

	return specs;
}
