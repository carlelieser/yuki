import { and, eq, inArray, notInArray, sql } from 'drizzle-orm';
import { schema } from '@yuki/db';
import type { ListingVersionAsset } from '@yuki/db/schema';
import { pickApkAsset, type MappedAsset } from '@yuki/github';
import type { AnyPgColumn } from 'drizzle-orm/pg-core';
import type { Transaction } from './publication.ts';

export async function upsertVersionAssets(
	tx: Transaction,
	versionId: string,
	assets: MappedAsset[]
): Promise<void> {
	const stored = schema.listingVersionAssets;
	const names = assets.map((asset) => asset.name);

	await tx
		.delete(stored)
		.where(
			names.length === 0
				? eq(stored.versionId, versionId)
				: and(eq(stored.versionId, versionId), notInArray(stored.name, names))
		);

	for (const asset of assets) {
		await tx
			.insert(stored)
			.values({ versionId, ...asset })
			.onConflictDoUpdate({
				target: [stored.versionId, stored.name],
				set: {
					downloadUrl: asset.downloadUrl,
					size: asset.size,
					downloadCount: asset.downloadCount,
					...identityUnlessAssetChanged(),
					updatedAt: new Date()
				}
			});
	}
}

function identityUnlessAssetChanged() {
	const stored = schema.listingVersionAssets;
	const isAssetChanged = sql`(${stored.downloadUrl} is distinct from excluded.download_url or ${stored.size} is distinct from excluded.size)`;
	const kept = (column: AnyPgColumn, reset: unknown) =>
		sql`case when ${isAssetChanged} then ${reset} else ${column} end`;

	return {
		packageName: kept(stored.packageName, null),
		signerDigests: kept(stored.signerDigests, null),
		lineageDigests: kept(stored.lineageDigests, null),
		isForeign: kept(stored.isForeign, false),
		identityReadAt: kept(stored.identityReadAt, null)
	};
}

type ReleaseAsset = Pick<
	ListingVersionAsset,
	'versionId' | 'name' | 'downloadUrl' | 'size' | 'downloadCount' | 'isForeign'
>;

export async function settleReleaseDownloads(tx: Transaction, listingId: string): Promise<void> {
	const releases = groupByVersion(await releaseAssets(tx, listingId));

	for (const [versionId, assets] of releases) {
		await tx
			.update(schema.listingVersions)
			.set(associatedDownload(assets))
			.where(eq(schema.listingVersions.id, versionId));
	}
}

async function releaseAssets(tx: Transaction, listingId: string): Promise<ReleaseAsset[]> {
	const stored = schema.listingVersionAssets;
	const versions = schema.listingVersions;
	const versionIds = tx
		.select({ id: versions.id })
		.from(versions)
		.where(eq(versions.listingId, listingId));

	return tx
		.select({
			versionId: stored.versionId,
			name: stored.name,
			downloadUrl: stored.downloadUrl,
			size: stored.size,
			downloadCount: stored.downloadCount,
			isForeign: stored.isForeign
		})
		.from(stored)
		.where(inArray(stored.versionId, versionIds));
}

function groupByVersion(assets: ReleaseAsset[]): Map<string, ReleaseAsset[]> {
	const releases = new Map<string, ReleaseAsset[]>();
	for (const asset of assets) {
		releases.set(asset.versionId, [...(releases.get(asset.versionId) ?? []), asset]);
	}

	return releases;
}

export function associatedDownload(assets: ReleaseAsset[]) {
	const own = assets.filter((asset) => !asset.isForeign);
	const picked = pickApkAsset(own);

	return {
		downloadUrl: picked?.downloadUrl ?? null,
		assetName: picked?.name ?? null,
		assetSize: picked?.size ?? null,
		downloadCount: picked?.downloadCount ?? 0,
		isIgnored: assets.length > 0 && own.length === 0
	};
}
