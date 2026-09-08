import { and, asc, desc, eq, isNotNull } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

export type ListingSummary = {
	id: string;
	slug: string;
	title: string;
	author: string;
	description: string | null;
	iconUrl: string | null;
	bannerUrl: string | null;
	stars: number;
};

export type ListingDetail = ListingSummary & {
	authorUrl: string;
	repositoryUrl: string;
	homepageUrl: string | null;
	license: string | null;
	isArchived: boolean;
	screenshots: { url: string; alt: string | null }[];
	versions: {
		tag: string;
		name: string | null;
		downloadUrl: string | null;
		assetName: string | null;
		isPrerelease: boolean;
		publishedAt: Date | null;
	}[];
};

export const summaryColumns = {
	id: schema.listings.id,
	slug: schema.listings.slug,
	title: schema.listings.title,
	author: schema.listings.author,
	description: schema.listings.description,
	iconUrl: schema.listings.iconUrl,
	bannerUrl: schema.listings.bannerUrl,
	stars: schema.listings.stars
};

export async function getFeaturedListings(db: Database, limit: number): Promise<ListingSummary[]> {
	return db
		.select(summaryColumns)
		.from(schema.listings)
		.where(
			and(
				eq(schema.listings.isPublished, true),
				isNotNull(schema.listings.iconUrl),
				isNotNull(schema.listings.bannerUrl)
			)
		)
		.orderBy(desc(schema.listings.stars))
		.limit(limit);
}

export async function getRecentListings(db: Database, limit: number): Promise<ListingSummary[]> {
	return db
		.select(summaryColumns)
		.from(schema.listings)
		.where(eq(schema.listings.isPublished, true))
		.orderBy(desc(schema.listings.createdAt))
		.limit(limit);
}

export async function getListingBySlug(db: Database, slug: string): Promise<ListingDetail | null> {
	const listing = await db.query.listings.findFirst({
		where: and(eq(schema.listings.slug, slug), eq(schema.listings.isPublished, true)),
		with: {
			screenshots: { orderBy: (table, { asc }) => [asc(table.position)] },
			versions: { orderBy: (table, { desc: order }) => [order(table.publishedAt)] }
		}
	});

	if (listing === undefined) return null;

	return {
		id: listing.id,
		slug: listing.slug,
		title: listing.title,
		author: listing.author,
		authorUrl: listing.authorUrl,
		description: listing.description,
		iconUrl: listing.iconUrl,
		bannerUrl: listing.bannerUrl,
		stars: listing.stars,
		repositoryUrl: listing.repositoryUrl,
		homepageUrl: listing.homepageUrl,
		license: listing.license,
		isArchived: listing.isArchived,
		screenshots: listing.screenshots.map((screenshot) => ({
			url: screenshot.url,
			alt: screenshot.alt
		})),
		versions: listing.versions.map((version) => ({
			tag: version.tag,
			name: version.name,
			downloadUrl: version.downloadUrl,
			assetName: version.assetName,
			isPrerelease: version.isPrerelease,
			publishedAt: version.publishedAt
		}))
	};
}

export type ListingPage = {
	results: ListingSummary[];
	hasMore: boolean;
};

export async function getListingsPage(
	db: Database,
	page: { limit: number; offset: number }
): Promise<ListingPage> {
	const rows = await db
		.select(summaryColumns)
		.from(schema.listings)
		.where(eq(schema.listings.isPublished, true))
		.orderBy(desc(schema.listings.stars), asc(schema.listings.id))
		.limit(page.limit + 1)
		.offset(page.offset);

	const results = rows.slice(0, page.limit);

	return { results, hasMore: rows.length > page.limit };
}
