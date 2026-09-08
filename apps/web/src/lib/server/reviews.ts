import { and, asc, desc, eq, sql } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

export type ReviewAuthor = {
	id: string;
	name: string;
	image: string | null;
};

export type Review = {
	id: string;
	rating: number;
	body: string | null;
	createdAt: Date;
	author: ReviewAuthor;
};

export type RatingBucket = {
	rating: number;
	count: number;
};

export type RatingSummary = {
	average: number;
	total: number;
	distribution: RatingBucket[];
};

export type ReviewPage = {
	results: Review[];
	hasMore: boolean;
};

const reviewColumns = {
	id: schema.listingReviews.id,
	rating: schema.listingReviews.rating,
	body: schema.listingReviews.body,
	createdAt: schema.listingReviews.createdAt,
	authorId: schema.user.id,
	authorName: schema.user.name,
	authorImage: schema.user.image
};

type ReviewRow = {
	id: string;
	rating: number;
	body: string | null;
	createdAt: Date;
	authorId: string;
	authorName: string;
	authorImage: string | null;
};

function toReview(row: ReviewRow): Review {
	return {
		id: row.id,
		rating: row.rating,
		body: row.body,
		createdAt: row.createdAt,
		author: { id: row.authorId, name: row.authorName, image: row.authorImage }
	};
}

export async function getRatingSummary(db: Database, listingId: string): Promise<RatingSummary> {
	const counted = (rating: number) =>
		sql<number>`count(*) filter (where ${schema.listingReviews.rating} = ${rating})`.mapWith(
			Number
		);

	const [row] = await db
		.select({
			total: sql<number>`count(*)`.mapWith(Number),
			average: sql<number>`coalesce(avg(${schema.listingReviews.rating}), 0)`.mapWith(Number),
			five: counted(5),
			four: counted(4),
			three: counted(3),
			two: counted(2),
			one: counted(1)
		})
		.from(schema.listingReviews)
		.where(eq(schema.listingReviews.listingId, listingId));

	return {
		average: row ? Math.round(row.average * 10) / 10 : 0,
		total: row?.total ?? 0,
		distribution: [
			{ rating: 5, count: row?.five ?? 0 },
			{ rating: 4, count: row?.four ?? 0 },
			{ rating: 3, count: row?.three ?? 0 },
			{ rating: 2, count: row?.two ?? 0 },
			{ rating: 1, count: row?.one ?? 0 }
		]
	};
}

export async function getReviewsPage(
	db: Database,
	listingId: string,
	page: { limit: number; offset: number }
): Promise<ReviewPage> {
	const rows = await db
		.select(reviewColumns)
		.from(schema.listingReviews)
		.innerJoin(schema.user, eq(schema.listingReviews.userId, schema.user.id))
		.where(eq(schema.listingReviews.listingId, listingId))
		.orderBy(desc(schema.listingReviews.createdAt), asc(schema.listingReviews.id))
		.limit(page.limit + 1)
		.offset(page.offset);

	return {
		results: rows.slice(0, page.limit).map(toReview),
		hasMore: rows.length > page.limit
	};
}

export async function getUserReview(
	db: Database,
	listingId: string,
	userId: string
): Promise<Review | null> {
	const [row] = await db
		.select(reviewColumns)
		.from(schema.listingReviews)
		.innerJoin(schema.user, eq(schema.listingReviews.userId, schema.user.id))
		.where(
			and(eq(schema.listingReviews.listingId, listingId), eq(schema.listingReviews.userId, userId))
		)
		.limit(1);

	return row ? toReview(row) : null;
}

export async function hasDownloadedListing(
	db: Database,
	listingId: string,
	userId: string
): Promise<boolean> {
	const rows = await db
		.select({ exists: sql<number>`1` })
		.from(schema.listingDownloads)
		.where(
			and(
				eq(schema.listingDownloads.listingId, listingId),
				eq(schema.listingDownloads.userId, userId)
			)
		)
		.limit(1);

	return rows.length > 0;
}

export async function recordDownload(
	db: Database,
	input: { listingId: string; userId: string; versionTag: string | null }
): Promise<void> {
	await db
		.insert(schema.listingDownloads)
		.values({
			listingId: input.listingId,
			userId: input.userId,
			versionTag: input.versionTag
		})
		.onConflictDoNothing({
			target: [schema.listingDownloads.listingId, schema.listingDownloads.userId]
		});
}

export async function upsertReview(
	db: Database,
	input: { listingId: string; userId: string; rating: number; body: string | null }
): Promise<void> {
	await db
		.insert(schema.listingReviews)
		.values({
			listingId: input.listingId,
			userId: input.userId,
			rating: input.rating,
			body: input.body
		})
		.onConflictDoUpdate({
			target: [schema.listingReviews.listingId, schema.listingReviews.userId],
			set: { rating: input.rating, body: input.body, updatedAt: new Date() }
		});
}

export async function deleteReview(db: Database, listingId: string, userId: string): Promise<void> {
	await db
		.delete(schema.listingReviews)
		.where(
			and(eq(schema.listingReviews.listingId, listingId), eq(schema.listingReviews.userId, userId))
		);
}
