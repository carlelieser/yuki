import { and, desc, eq, inArray, isNotNull, isNull, not, sql, type SQL } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import type { ApkIdentity } from '../apk/identity.ts';
import { PLATFORM_CERTIFICATES } from '../apk/publishers.ts';
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
			identityReadAt: new Date()
		})
		.where(eq(schema.listingVersionAssets.id, assetId));
}

export type Reclassification = {
	listingIds: string[];
	foreignCount: number;
	ownCount: number;
};

export function reclassifyQuery(): SQL {
	const assets = schema.listingVersionAssets;
	const versions = schema.listingVersions;
	const platformCertificates = sql`${sql.param(PLATFORM_CERTIFICATES)}::text[]`;
	const isPlatformSigned = sql`coalesce(cardinality(${assets.signerDigests}) > 0 and ${assets.signerDigests} <@ ${platformCertificates}, false)`;

	return sql`update ${assets}
		set ${sql.identifier(assets.isForeign.name)} = ${isPlatformSigned}
		from ${versions}
		where ${versions.id} = ${assets.versionId}
			and ${assets.identityReadAt} is not null
			and ${assets.isForeign} is distinct from ${isPlatformSigned}
		returning ${versions.listingId} as listing_id, ${assets.isForeign} as is_foreign`;
}

export async function reclassifyAssets(db: Database): Promise<Reclassification> {
	const result = await db.execute<{ listing_id: string; is_foreign: boolean }>(reclassifyQuery());
	const rows = result.rows;

	return {
		listingIds: [...new Set(rows.map((row) => row.listing_id))],
		foreignCount: rows.filter((row) => row.is_foreign).length,
		ownCount: rows.filter((row) => !row.is_foreign).length
	};
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
