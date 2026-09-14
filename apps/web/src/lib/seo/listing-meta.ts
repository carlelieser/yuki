import { SITE_NAME } from '../site.ts';
import { describeListing, type PageMeta } from './page-meta.ts';
import type { ListingDetail } from '../server/listings.ts';

const AMBIGUOUS_TITLE_LENGTH = 8;

export function listingImageUrl(listing: ListingDetail): string | null {
	return listing.iconUrl ?? listing.bannerUrl;
}

export function listingHeadline(listing: Pick<ListingDetail, 'title' | 'author'>): string {
	const isSingleWord = !listing.title.trim().includes(' ');
	const isShort = listing.title.length <= AMBIGUOUS_TITLE_LENGTH;
	const isDistinctFromAuthor = listing.title.toLowerCase() !== listing.author.toLowerCase();

	if (!isSingleWord || !isShort) return listing.title;

	return isDistinctFromAuthor ? `${listing.title} by ${listing.author}` : listing.title;
}

export function listingMeta(listing: ListingDetail): PageMeta {
	return {
		title: `${listingHeadline(listing)} · ${SITE_NAME}`,
		description: describeListing(listing),
		canonicalPath: `/listings/${listing.slug}`,
		imageUrl: listingImageUrl(listing)
	};
}
