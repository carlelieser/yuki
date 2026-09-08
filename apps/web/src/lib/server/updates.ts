import { and, eq, inArray, sql } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import { isNewerTag } from '../version-compare.ts';
import type { InstalledApp } from '../schemas/updates.ts';

export type AvailableUpdate = {
	packageName: string;
	slug: string;
	title: string;
	iconUrl: string | null;
	installedTag: string;
	tag: string;
	name: string | null;
	notes: string | null;
	downloadUrl: string;
	assetName: string | null;
	assetSize: number | null;
	isPrerelease: boolean;
	publishedAt: Date | null;
};

type CandidateRow = {
	packageName: string | null;
	slug: string;
	title: string;
	iconUrl: string | null;
	tag: string;
	name: string | null;
	notes: string | null;
	downloadUrl: string | null;
	assetName: string | null;
	assetSize: number | null;
	isPrerelease: boolean;
	publishedAt: Date | null;
};

export async function findAvailableUpdates(
	db: Database,
	installed: InstalledApp[],
	options: { includePrereleases: boolean }
): Promise<AvailableUpdate[]> {
	if (installed.length === 0) return [];

	const byPackage = new Map(installed.map((entry) => [entry.packageName, entry]));

	const releaseFilter = options.includePrereleases
		? undefined
		: eq(schema.listingVersions.isPrerelease, false);

	const rows: CandidateRow[] = await db
		.select({
			packageName: schema.listings.packageName,
			slug: schema.listings.slug,
			title: schema.listings.title,
			iconUrl: schema.listings.iconUrl,
			tag: schema.listingVersions.tag,
			name: schema.listingVersions.name,
			notes: schema.listingVersions.notes,
			downloadUrl: schema.listingVersions.downloadUrl,
			assetName: schema.listingVersions.assetName,
			assetSize: schema.listingVersions.assetSize,
			isPrerelease: schema.listingVersions.isPrerelease,
			publishedAt: schema.listingVersions.publishedAt
		})
		.from(schema.listingVersions)
		.innerJoin(schema.listings, eq(schema.listingVersions.listingId, schema.listings.id))
		.where(
			and(
				eq(schema.listings.isPublished, true),
				inArray(schema.listings.packageName, [...byPackage.keys()]),
				sql`${schema.listingVersions.downloadUrl} is not null`,
				releaseFilter
			)
		);

	const best = new Map<string, AvailableUpdate>();

	for (const row of rows) {
		if (row.packageName === null || row.downloadUrl === null) continue;

		const current = byPackage.get(row.packageName);
		if (current === undefined) continue;

		if (!isNewerTag(row.tag, current.versionTag)) continue;

		const existing = best.get(row.packageName);
		if (existing !== undefined && !isNewerTag(row.tag, existing.tag)) continue;

		best.set(row.packageName, {
			packageName: row.packageName,
			slug: row.slug,
			title: row.title,
			iconUrl: row.iconUrl,
			installedTag: current.versionTag,
			tag: row.tag,
			name: row.name,
			notes: row.notes,
			downloadUrl: row.downloadUrl,
			assetName: row.assetName,
			assetSize: row.assetSize,
			isPrerelease: row.isPrerelease,
			publishedAt: row.publishedAt
		});
	}

	return [...best.values()].sort((left, right) =>
		left.packageName.localeCompare(right.packageName)
	);
}
