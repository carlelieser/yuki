import { SITE_NAME } from '../site.ts';
import { describeListing, type PageMeta } from './page-meta.ts';
import type { ListingDetail } from '../server/listings.ts';

export function listingImageUrl(listing: ListingDetail): string | null {
	return listing.iconUrl ?? listing.bannerUrl;
}

export function listingMeta(listing: ListingDetail): PageMeta {
	return {
		title: `${listing.title} · ${SITE_NAME}`,
		description: describeListing(listing),
		canonicalPath: `/listings/${listing.slug}`,
		imageUrl: listingImageUrl(listing)
	};
}
