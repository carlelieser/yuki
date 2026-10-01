import { and, eq, notInArray, sql } from 'drizzle-orm';
import { schema } from '@yuki/db';
import type { ListingVersionAsset } from '@yuki/db/schema';
import { pickApkAsset, type MappedAsset } from '@yuki/github';
import type { AnyPgColumn } from 'drizzle-orm/pg-core';
import { primaryPackage, type PackagedAsset } from '../releases/primary-package.ts';
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
	'versionId' | 'name' | 'downloadUrl' | 'size' | 'downloadCount' | 'isForeign' | 'packageName'
> & { signer: string | null; publishedAt: Date | null; isPrerelease: boolean };

export async function settleReleaseDownloads(
	tx: Transaction,
	listingId: string
): Promise<string | null> {
	const assets = await releaseAssets(tx, listingId);
	const primary = await listingPrimaryPackage(tx, listingId, assets);

	for (const [versionId, releaseAssets] of groupByVersion(assets)) {
		await tx
			.update(schema.listingVersions)
			.set(associatedDownload(releaseAssets, primary))
			.where(eq(schema.listingVersions.id, versionId));
	}

	return primary;
}

async function listingPrimaryPackage(
	tx: Transaction,
	listingId: string,
	assets: ReleaseAsset[]
): Promise<string | null> {
	const [repository] = await tx
		.select({
			githubRepoId: schema.listings.githubRepoId,
			owner: schema.listings.owner,
			name: schema.listings.name
		})
		.from(schema.listings)
		.where(eq(schema.listings.id, listingId))
		.limit(1);
	if (repository === undefined) return null;

	return primaryPackage(assets.filter(isPackagedOwnAsset), repository);
}

function isPackagedOwnAsset(asset: ReleaseAsset): asset is ReleaseAsset & PackagedAsset {
	return !asset.isForeign && asset.packageName !== null;
}

async function releaseAssets(tx: Transaction, listingId: string): Promise<ReleaseAsset[]> {
	const stored = schema.listingVersionAssets;
	const versions = schema.listingVersions;

	return tx
		.select({
			versionId: stored.versionId,
			name: stored.name,
			downloadUrl: stored.downloadUrl,
			size: stored.size,
			downloadCount: stored.downloadCount,
			isForeign: stored.isForeign,
			packageName: stored.packageName,
			signer: sql<string | null>`${stored.signerDigests}[1]`,
			publishedAt: versions.publishedAt,
			isPrerelease: versions.isPrerelease
		})
		.from(stored)
		.innerJoin(versions, eq(versions.id, stored.versionId))
		.where(eq(versions.listingId, listingId));
}

function groupByVersion(assets: ReleaseAsset[]): Map<string, ReleaseAsset[]> {
	const releases = new Map<string, ReleaseAsset[]>();
	for (const asset of assets) {
		releases.set(asset.versionId, [...(releases.get(asset.versionId) ?? []), asset]);
	}

	return releases;
}

export function associatedDownload(assets: ReleaseAsset[], primary: string | null) {
	const own = assets.filter((asset) => !asset.isForeign);
	const primaryBuilds = own.filter((asset) => asset.packageName === primary);
	const picked = pickApkAsset(primaryBuilds.length > 0 ? primaryBuilds : own);

	return {
		downloadUrl: picked?.downloadUrl ?? null,
		assetName: picked?.name ?? null,
		assetSize: picked?.size ?? null,
		downloadCount: picked?.downloadCount ?? 0,
		isIgnored: assets.length > 0 && own.length === 0
	};
}
