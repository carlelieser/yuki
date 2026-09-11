import { and, asc, desc, eq, isNotNull, sql, type AnyColumn, type SQL } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import type { ListingCategory } from '$lib/categories.ts';
import { getRatingSummary } from './reviews.ts';
import {
	DEFAULT_BROWSE_SORT,
	defaultOrderFor,
	type BrowseOrder,
	type BrowseSort
} from '../browse.ts';

export type ListingSummary = {
	id: string;
	githubRepoId: number;
	slug: string;
	title: string;
	author: string;
	description: string | null;
	iconUrl: string | null;
	bannerUrl: string | null;
	stars: number;
	category: ListingCategory | null;
	ratingAverage: number | null;
	ratingCount: number;
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

const ratingAverage = sql<number | null>`(
	select round(avg(${schema.listingReviews.rating})::numeric, 1)
	from ${schema.listingReviews}
	where ${schema.listingReviews.listingId} = ${sql`${schema.listings}.${sql.identifier('id')}`}
)`.mapWith(Number);

const ratingCount = sql<number>`(
	select count(*)
	from ${schema.listingReviews}
	where ${schema.listingReviews.listingId} = ${sql`${schema.listings}.${sql.identifier('id')}`}
)`.mapWith(Number);

export const summaryColumns = {
	id: schema.listings.id,
	githubRepoId: schema.listings.githubRepoId,
	slug: schema.listings.slug,
	title: schema.listings.title,
	author: schema.listings.author,
	description: schema.listings.description,
	iconUrl: schema.listings.iconUrl,
	bannerUrl: schema.listings.bannerUrl,
	stars: schema.listings.stars,
	category: schema.listings.category,
	ratingAverage,
	ratingCount
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

	const rating = await getRatingSummary(db, listing.id);

	return {
		id: listing.id,
		githubRepoId: listing.githubRepoId,
		slug: listing.slug,
		title: listing.title,
		author: listing.author,
		authorUrl: listing.authorUrl,
		description: listing.description,
		iconUrl: listing.iconUrl,
		bannerUrl: listing.bannerUrl,
		stars: listing.stars,
		category: listing.category,
		ratingAverage: rating.total === 0 ? null : rating.average,
		ratingCount: rating.total,
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

export type CategorySection = {
	category: ListingCategory;
	results: ListingSummary[];
};

export type RankedRow = ListingSummary & { rank: number };

function summaryOf(row: RankedRow): ListingSummary {
	return {
		id: row.id,
		githubRepoId: row.githubRepoId,
		slug: row.slug,
		title: row.title,
		author: row.author,
		description: row.description,
		iconUrl: row.iconUrl,
		bannerUrl: row.bannerUrl,
		category: row.category,
		stars: row.stars,
		ratingAverage: row.ratingAverage,
		ratingCount: row.ratingCount
	};
}

export function groupIntoSections(rows: RankedRow[], limit: number): CategorySection[] {
	const sections = new Map<ListingCategory, ListingSummary[]>();

	for (const row of rows) {
		if (row.category === null) continue;
		const results = sections.get(row.category) ?? [];
		if (results.length >= limit) continue;
		results.push(summaryOf(row));
		sections.set(row.category, results);
	}

	return [...sections].map(([category, results]) => ({ category, results }));
}

const SORT_COLUMNS: Record<BrowseSort, AnyColumn | SQL> = {
	stars: schema.listings.stars,
	newest: schema.listings.createdAt,
	updated: schema.listings.repoPushedAt,
	name: sql`lower(${schema.listings.title})`
};

const NULLABLE_SORTS = new Set<BrowseSort>(['updated']);

export function orderByFor(sort: BrowseSort, order: BrowseOrder): SQL[] {
	const column = SORT_COLUMNS[sort];
	const direction = order === 'asc' ? asc(column) : desc(column);
	const clause = NULLABLE_SORTS.has(sort) ? sql`${direction} nulls last` : direction;

	return [clause, asc(schema.listings.id)];
}

export async function getListingsPage(
	db: Database,
	page: {
		limit: number;
		offset: number;
		sort?: BrowseSort;
		order?: BrowseOrder;
		category?: ListingCategory | null;
	}
): Promise<ListingPage> {
	const sort = page.sort ?? DEFAULT_BROWSE_SORT;
	const order = page.order ?? defaultOrderFor(sort);
	const category = page.category ?? null;

	const rows = await db
		.select(summaryColumns)
		.from(schema.listings)
		.where(
			category === null
				? eq(schema.listings.isPublished, true)
				: and(eq(schema.listings.isPublished, true), eq(schema.listings.category, category))
		)
		.orderBy(...orderByFor(sort, order))
		.limit(page.limit + 1)
		.offset(page.offset);

	const results = rows.slice(0, page.limit);

	return { results, hasMore: rows.length > page.limit };
}

export async function getCategorySections(db: Database, limit: number): Promise<CategorySection[]> {
	const ranked = db
		.select({
			...summaryColumns,
			ratingAverage: ratingAverage.as('rating_average'),
			ratingCount: ratingCount.as('rating_count'),
			rank: sql<number>`row_number() over (
				partition by ${schema.listings.category}
				order by ${schema.listings.stars} desc, ${schema.listings.id} asc
			)`
				.mapWith(Number)
				.as('rank')
		})
		.from(schema.listings)
		.where(and(eq(schema.listings.isPublished, true), isNotNull(schema.listings.category)))
		.as('ranked');

	const rows = await db
		.select()
		.from(ranked)
		.where(sql`${ranked.rank} <= ${limit}`)
		.orderBy(asc(ranked.category), asc(ranked.rank));

	return groupIntoSections(rows as RankedRow[], limit);
}
