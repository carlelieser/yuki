import { and, desc, eq, inArray, isNotNull, isNull, not, sql } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import type { ApkIdentity } from '../apk/identity.ts';
import { settleReleaseDownloads } from './assets.ts';
import { settlePublication, type Transaction } from './publication.ts';
import { settleLatestRelease } from './releases.ts';

export type PendingAsset = {
	id: string;
	listingId: string;
	downloadUrl: string;
};

export async function listPendingIdentities(
	db: Database,
	limit: number,
	listingIds?: string[]
): Promise<PendingAsset[]> {
	const assets = schema.listingVersionAssets;
	const versions = schema.listingVersions;
	const pending = isNull(assets.identityReadAt);

	return db
		.select({ id: assets.id, listingId: versions.listingId, downloadUrl: assets.downloadUrl })
		.from(assets)
		.innerJoin(versions, eq(versions.id, assets.versionId))
		.where(
			listingIds === undefined ? pending : and(pending, inArray(versions.listingId, listingIds))
		)
		.orderBy(sql`${versions.publishedAt} desc nulls last`, desc(versions.createdAt))
		.limit(limit);
}

export async function saveIdentity(
	db: Database,
	assetId: string,
	identity: ApkIdentity | null
): Promise<void> {
	await db
		.update(schema.listingVersionAssets)
		.set({
			packageName: identity?.packageName ?? null,
			signerDigests: identity?.signers ?? null,
			lineageDigests: identity?.lineage ?? null,
			isForeign: identity?.isForeign ?? false,
			identityReadAt: new Date()
		})
		.where(eq(schema.listingVersionAssets.id, assetId));
}

export async function settleListings(db: Database, listingIds: string[]): Promise<void> {
	for (const listingId of listingIds) {
		await db.transaction(async (tx) => {
			await settleReleaseDownloads(tx, listingId);
			await settlePackageName(tx, listingId);
			await settleLatestRelease(tx, listingId);
			await settlePublication(tx, listingId);
		});
	}
}

async function settlePackageName(tx: Transaction, listingId: string): Promise<void> {
	const assets = schema.listingVersionAssets;
	const versions = schema.listingVersions;

	const [newest] = await tx
		.select({ packageName: assets.packageName })
		.from(assets)
		.innerJoin(versions, eq(versions.id, assets.versionId))
		.where(
			and(eq(versions.listingId, listingId), not(assets.isForeign), isNotNull(assets.packageName))
		)
		.orderBy(
			versions.isPrerelease,
			sql`${versions.publishedAt} desc nulls last`,
			desc(versions.createdAt)
		)
		.limit(1);

	if (newest?.packageName == null) return;

	await tx
		.update(schema.listings)
		.set({ packageName: newest.packageName })
		.where(eq(schema.listings.id, listingId));
}
