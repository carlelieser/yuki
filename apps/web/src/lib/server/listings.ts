import { and, asc, desc, eq, isNotNull, sql, type AnyColumn, type SQL } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import {
	DEFAULT_BROWSE_SORT,
	defaultOrderFor,
	type BrowseOrder,
	type BrowseSort
} from '../browse.ts';

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

const SORT_COLUMNS: Record<BrowseSort, AnyColumn | SQL> = {
	stars: schema.listings.stars,
	newest: schema.listings.createdAt,
	updated: schema.listings.repoPushedAt,
	name: sql`lower(${schema.listings.title})`
};

const NULLABLE_SORTS = new Set<BrowseSort>(['updated']);

function orderByFor(sort: BrowseSort, order: BrowseOrder): SQL[] {
	const column = SORT_COLUMNS[sort];
	const direction = order === 'asc' ? asc(column) : desc(column);
	const clause = NULLABLE_SORTS.has(sort) ? sql`${direction} nulls last` : direction;

	return [clause, asc(schema.listings.id)];
}

export async function getListingsPage(
	db: Database,
	page: { limit: number; offset: number; sort?: BrowseSort; order?: BrowseOrder }
): Promise<ListingPage> {
	const sort = page.sort ?? DEFAULT_BROWSE_SORT;
	const order = page.order ?? defaultOrderFor(sort);

	const rows = await db
		.select(summaryColumns)
		.from(schema.listings)
		.where(eq(schema.listings.isPublished, true))
		.orderBy(...orderByFor(sort, order))
		.limit(page.limit + 1)
		.offset(page.offset);

	const results = rows.slice(0, page.limit);

	return { results, hasMore: rows.length > page.limit };
}
