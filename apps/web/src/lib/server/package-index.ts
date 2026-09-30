import { and, eq, isNotNull, not, sql } from 'drizzle-orm';
import { QueryBuilder } from 'drizzle-orm/pg-core';
import { schema, type Database } from '@yuki/db';

export type PackageIndexEntry = {
	packageName: string;
	githubRepoId: number;
	slug: string;
	title: string;
	iconUrl: string | null;
};

type Builder = Pick<Database, 'select' | 'selectDistinct'>;

const indexColumns = {
	packageName: schema.listings.packageName,
	githubRepoId: schema.listings.githubRepoId,
	slug: schema.listings.slug,
	title: schema.listings.title,
	iconUrl: schema.listings.iconUrl
};

function selectIndex(builder: Builder) {
	const claimed = builder
		.select({ packageName: schema.listings.packageName })
		.from(schema.listings)
		.where(and(eq(schema.listings.isPublished, true), isNotNull(schema.listings.packageName)))
		.groupBy(schema.listings.packageName)
		.having(sql`count(*) = 1`)
		.as('claimed');

	return builder
		.select(indexColumns)
		.from(schema.listings)
		.innerJoin(claimed, eq(claimed.packageName, schema.listings.packageName))
		.where(eq(schema.listings.isPublished, true))
		.orderBy(schema.listings.packageName);
}

export function packageIndexQuery(): { sql: string } {
	return selectIndex(new QueryBuilder() as unknown as Builder).toSQL();
}

export async function getPackageIndex(db: Database): Promise<PackageIndexEntry[]> {
	const rows = await selectIndex(db);

	return rows.filter((row): row is PackageIndexEntry => row.packageName !== null);
}

export type SigningIdentity = {
	signers: string[];
	lineage: string[];
};

export type PackageIdentityEntry = PackageIndexEntry & {
	identities: SigningIdentity[];
};

export type IdentityRow = PackageIndexEntry & {
	signers: string[];
	lineage: string[] | null;
};

function selectIdentityRows(builder: Builder) {
	const assets = schema.listingVersionAssets;
	const versions = schema.listingVersions;

	return builder
		.selectDistinct({
			...indexColumns,
			packageName: assets.packageName,
			signers: assets.signerDigests,
			lineage: assets.lineageDigests
		})
		.from(assets)
		.innerJoin(versions, eq(versions.id, assets.versionId))
		.innerJoin(schema.listings, eq(schema.listings.id, versions.listingId))
		.where(
			and(
				eq(schema.listings.isPublished, true),
				not(assets.isForeign),
				isNotNull(assets.packageName),
				sql`cardinality(${assets.signerDigests}) > 0`
			)
		);
}

export function packageIdentityQuery(): { sql: string } {
	return selectIdentityRows(new QueryBuilder() as unknown as Builder).toSQL();
}

export async function getPackageIdentities(db: Database): Promise<PackageIdentityEntry[]> {
	const rows = await selectIdentityRows(db);

	return buildIdentityIndex(
		rows.filter((row): row is IdentityRow => row.packageName !== null && row.signers !== null)
	);
}

export function buildIdentityIndex(rows: IdentityRow[]): PackageIdentityEntry[] {
	const listings = groupBy(soleClaims(rows), (row) => `${row.githubRepoId}:${row.packageName}`);

	return [...listings.values()]
		.map(toIdentityEntry)
		.sort(
			(left, right) =>
				left.packageName.localeCompare(right.packageName) || left.githubRepoId - right.githubRepoId
		);
}

function soleClaims(rows: IdentityRow[]): IdentityRow[] {
	const claimants = groupBy(rows, identityKey);

	return rows.filter((row) => {
		const claims = claimants.get(identityKey(row)) ?? [];
		return new Set(claims.map((claim) => claim.githubRepoId)).size === 1;
	});
}

function toIdentityEntry(rows: IdentityRow[]): PackageIdentityEntry {
	const [first] = rows as [IdentityRow, ...IdentityRow[]];
	const identities = [...groupBy(rows, identityKey).values()].map(mergeIdentity);

	return {
		packageName: first.packageName,
		githubRepoId: first.githubRepoId,
		slug: first.slug,
		title: first.title,
		iconUrl: first.iconUrl,
		identities
	};
}

function mergeIdentity(rows: IdentityRow[]): SigningIdentity {
	const signers = [...new Set(rows[0]?.signers ?? [])].sort();
	const lineage = new Set(rows.flatMap((row) => row.lineage ?? []));

	return { signers, lineage: [...lineage].filter((digest) => !signers.includes(digest)).sort() };
}

function identityKey(row: IdentityRow): string {
	return `${row.packageName}|${[...row.signers].sort().join(',')}`;
}

function groupBy<Item>(items: Item[], keyOf: (item: Item) => string): Map<string, Item[]> {
	const groups = new Map<string, Item[]>();
	for (const item of items) {
		const key = keyOf(item);
		groups.set(key, [...(groups.get(key) ?? []), item]);
	}

	return groups;
}
