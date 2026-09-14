import { categoryLabel } from '../categories.ts';
import { siteUrl } from '../site.ts';
import { describeListing } from './page-meta.ts';
import type { ListingDetail } from '../server/listings.ts';

const UNSAFE_JSON = /[<>&\u2028\u2029]/g;

const JSON_ESCAPES: Record<string, string> = {
	'<': '\\u003c',
	'>': '\\u003e',
	'&': '\\u0026',
	'\u2028': '\\u2028',
	'\u2029': '\\u2029'
};

function escapeForScriptTag(serialized: string): string {
	return serialized.replace(UNSAFE_JSON, (character) => JSON_ESCAPES[character] ?? character);
}

function ratingOf(listing: ListingDetail): Record<string, unknown> | undefined {
	if (listing.ratingCount === 0 || listing.ratingAverage === null) return undefined;

	return {
		'@type': 'AggregateRating',
		ratingValue: listing.ratingAverage,
		ratingCount: listing.ratingCount
	};
}

function screenshotsOf(listing: ListingDetail): string[] | undefined {
	if (listing.screenshots.length === 0) return undefined;

	return listing.screenshots.map((screenshot) => screenshot.url);
}

export function toSoftwareApplicationSchema(listing: ListingDetail): string {
	const schema = {
		'@context': 'https://schema.org',
		'@type': 'SoftwareApplication',
		name: listing.title,
		description: describeListing(listing),
		url: siteUrl(`/listings/${listing.slug}`),
		applicationCategory: listing.category === null ? undefined : categoryLabel(listing.category),
		operatingSystem: 'Android',
		author: { '@type': 'Person', name: listing.author, url: listing.authorUrl },
		codeRepository: listing.repositoryUrl,
		license: listing.license ?? undefined,
		image: listing.iconUrl ?? undefined,
		screenshot: screenshotsOf(listing),
		softwareVersion: listing.versions[0]?.tag,
		aggregateRating: ratingOf(listing),
		offers: { '@type': 'Offer', price: 0, priceCurrency: 'USD' }
	};

	return escapeForScriptTag(JSON.stringify(schema));
}
