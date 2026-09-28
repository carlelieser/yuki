import { and, desc, eq, inArray, isNotNull, isNull, sql } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import type { AnyPgColumn } from 'drizzle-orm/pg-core';
import type { ApkIdentity } from '../apk/identity.ts';

export type PendingVersion = {
	id: string;
	listingId: string;
	downloadUrl: string;
};

export async function listPendingIdentities(
	db: Database,
	limit: number,
	listingIds?: string[]
): Promise<PendingVersion[]> {
	const versions = schema.listingVersions;
	const pending = and(isNull(versions.identityReadAt), isNotNull(versions.downloadUrl));

	const rows = await db
		.select({ id: versions.id, listingId: versions.listingId, downloadUrl: versions.downloadUrl })
		.from(versions)
		.where(
			listingIds === undefined ? pending : and(pending, inArray(versions.listingId, listingIds))
		)
		.orderBy(sql`${versions.publishedAt} desc nulls last`, desc(versions.createdAt))
		.limit(limit);

	return rows.filter((row): row is PendingVersion => row.downloadUrl !== null);
}

export async function saveIdentity(
	db: Database,
	versionId: string,
	identity: ApkIdentity | null
): Promise<void> {
	await db
		.update(schema.listingVersions)
		.set({
			packageName: identity?.packageName ?? null,
			signerDigests: identity?.signers ?? null,
			lineageDigests: identity?.lineage ?? null,
			identityReadAt: new Date()
		})
		.where(eq(schema.listingVersions.id, versionId));
}

export async function settleListingPackageNames(db: Database, listingIds: string[]): Promise<void> {
	if (listingIds.length === 0) return;

	const versions = schema.listingVersions;
	const newest = db
		.selectDistinctOn([versions.listingId], {
			listingId: versions.listingId,
			packageName: versions.packageName
		})
		.from(versions)
		.where(and(inArray(versions.listingId, listingIds), isNotNull(versions.packageName)))
		.orderBy(
			versions.listingId,
			versions.isPrerelease,
			sql`${versions.publishedAt} desc nulls last`,
			desc(versions.createdAt)
		)
		.as('newest');

	await db
		.update(schema.listings)
		.set({ packageName: sql`${newest.packageName}` })
		.from(newest)
		.where(eq(schema.listings.id, newest.listingId));
}

export function identityUnlessAssetChanged() {
	const versions = schema.listingVersions;
	const isAssetChanged = sql`(${versions.downloadUrl} is distinct from excluded.download_url or ${versions.assetSize} is distinct from excluded.asset_size)`;
	const kept = (column: AnyPgColumn) =>
		sql`case when ${isAssetChanged} then null else ${column} end`;

	return {
		packageName: kept(versions.packageName),
		signerDigests: kept(versions.signerDigests),
		lineageDigests: kept(versions.lineageDigests),
		identityReadAt: kept(versions.identityReadAt)
	};
}
