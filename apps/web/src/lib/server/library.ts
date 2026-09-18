import { and, desc, eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import { summaryColumns, type ListingSummary } from './listings.ts';

export type LibraryEntry = ListingSummary & {
	packageName: string | null;
	versionTag: string | null;
	addedAt: Date;
};

export async function findPublishedListingId(db: Database, slug: string): Promise<string | null> {
	const [row] = await db
		.select({ id: schema.listings.id })
		.from(schema.listings)
		.where(and(eq(schema.listings.slug, slug), eq(schema.listings.isPublished, true)))
		.limit(1);

	return row?.id ?? null;
}

export async function getLibrary(db: Database, userId: string): Promise<LibraryEntry[]> {
	return db
		.select({
			...summaryColumns,
			packageName: schema.listings.packageName,
			versionTag: schema.listingDownloads.versionTag,
			addedAt: schema.listingDownloads.createdAt
		})
		.from(schema.listingDownloads)
		.innerJoin(schema.listings, eq(schema.listings.id, schema.listingDownloads.listingId))
		.where(
			and(eq(schema.listingDownloads.userId, userId), eq(schema.listings.isPublished, true))
		)
		.orderBy(desc(schema.listingDownloads.createdAt));
}
