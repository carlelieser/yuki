import { categoryLabel, type ListingCategory } from '../categories.ts';
import { SITE_NAME, SITE_TAGLINE } from '../site.ts';

export const META_DESCRIPTION_LIMIT = 155;
const MIN_USABLE_DESCRIPTION = 40;

export type PageMeta = {
	title: string;
	description: string;
	canonicalPath: string;
	imageUrl?: string | null;
	isIndexable?: boolean;
};

type DescribableListing = {
	title: string;
	author: string;
	description: string | null;
	category: ListingCategory | null;
};

export function truncateForMeta(text: string, limit: number = META_DESCRIPTION_LIMIT): string {
	const collapsed = text.replace(/\s+/g, ' ').trim();
	if (collapsed.length <= limit) return collapsed;

	const clipped = collapsed.slice(0, limit - 1);
	const lastSpace = clipped.lastIndexOf(' ');

	return `${(lastSpace > 0 ? clipped.slice(0, lastSpace) : clipped).replace(/[,;:.]$/, '')}…`;
}

function composeDescription(listing: DescribableListing): string {
	const kind =
		listing.category === null ? 'app' : `${categoryLabel(listing.category).toLowerCase()} app`;

	return `${listing.title} by ${listing.author} — a free, open-source ${kind} for Android. Browse releases and download the latest version on ${SITE_NAME}.`;
}

export function describeListing(listing: DescribableListing): string {
	const provided = listing.description?.replace(/\s+/g, ' ').trim() ?? '';
	if (provided.length < MIN_USABLE_DESCRIPTION) return truncateForMeta(composeDescription(listing));

	return truncateForMeta(provided);
}

export function browseMeta(category: ListingCategory | null): PageMeta {
	if (category === null) {
		return {
			title: `Browse apps · ${SITE_NAME}`,
			description: `Browse every Shizuku-powered Android app on ${SITE_NAME}, sorted by stars, release date, or name.`,
			canonicalPath: '/browse'
		};
	}

	const label = categoryLabel(category);

	return {
		title: `${label} apps · ${SITE_NAME}`,
		description: `Shizuku-powered ${label.toLowerCase()} apps for Android, open source and free to download on ${SITE_NAME}.`,
		canonicalPath: `/browse?category=${category}`
	};
}

export function searchMeta(query: string, total: number): PageMeta {
	const title = query === '' ? `Search · ${SITE_NAME}` : `${query} · ${SITE_NAME}`;

	return {
		title,
		description: `${total} Shizuku-powered Android ${total === 1 ? 'app' : 'apps'} matching “${query}” on ${SITE_NAME}.`,
		canonicalPath: '/search',
		isIndexable: false
	};
}

export function siteMeta(): PageMeta {
	return {
		title: `${SITE_NAME} — ${SITE_TAGLINE}`,
		description: `${SITE_NAME} brings together Shizuku-powered Android apps from GitHub. Browse by category, read reviews, and download the latest release.`,
		canonicalPath: '/'
	};
}
