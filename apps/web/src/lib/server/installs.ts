import { and, desc, eq, isNull } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

export type InstalledListing = {
	listingId: string;
	slug: string;
	title: string;
	iconUrl: string | null;
	packageName: string;
	versionTag: string | null;
	versionCode: string | null;
	installedAt: Date;
};

export async function getInstalledListings(
	db: Database,
	userId: string
): Promise<InstalledListing[]> {
	return db
		.select({
			listingId: schema.listings.id,
			slug: schema.listings.slug,
			title: schema.listings.title,
			iconUrl: schema.listings.iconUrl,
			packageName: schema.listingInstalls.packageName,
			versionTag: schema.listingInstalls.versionTag,
			versionCode: schema.listingInstalls.versionCode,
			installedAt: schema.listingInstalls.installedAt
		})
		.from(schema.listingInstalls)
		.innerJoin(schema.listings, eq(schema.listingInstalls.listingId, schema.listings.id))
		.where(and(eq(schema.listingInstalls.userId, userId), isNull(schema.listingInstalls.removedAt)))
		.orderBy(desc(schema.listingInstalls.installedAt));
}

export async function recordInstall(
	db: Database,
	input: {
		listingId: string;
		userId: string;
		packageName: string;
		versionTag: string | null;
		versionCode: string | null;
	}
): Promise<void> {
	await db
		.insert(schema.listingInstalls)
		.values({
			listingId: input.listingId,
			userId: input.userId,
			packageName: input.packageName,
			versionTag: input.versionTag,
			versionCode: input.versionCode
		})
		.onConflictDoUpdate({
			target: [schema.listingInstalls.listingId, schema.listingInstalls.userId],
			set: {
				packageName: input.packageName,
				versionTag: input.versionTag,
				versionCode: input.versionCode,
				removedAt: null,
				updatedAt: new Date()
			}
		});
}

export async function recordUninstall(
	db: Database,
	input: { listingId: string; userId: string }
): Promise<void> {
	await db
		.update(schema.listingInstalls)
		.set({ removedAt: new Date(), updatedAt: new Date() })
		.where(
			and(
				eq(schema.listingInstalls.listingId, input.listingId),
				eq(schema.listingInstalls.userId, input.userId),
				isNull(schema.listingInstalls.removedAt)
			)
		);
}
