import { and, desc, eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

export type ListingSummary = {
	id: string;
	slug: string;
	title: string;
	author: string;
	description: string | null;
	iconUrl: string | null;
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
	stars: schema.listings.stars
};

export async function getFeaturedListings(db: Database, limit: number): Promise<ListingSummary[]> {
	return db
		.select(summaryColumns)
		.from(schema.listings)
		.where(eq(schema.listings.isPublished, true))
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
