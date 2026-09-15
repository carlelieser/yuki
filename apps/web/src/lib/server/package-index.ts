import { and, eq, isNotNull, sql } from 'drizzle-orm';
import { QueryBuilder } from 'drizzle-orm/pg-core';
import { schema, type Database } from '@yuki/db';

export type PackageIndexEntry = {
	packageName: string;
	githubRepoId: number;
	slug: string;
	title: string;
	iconUrl: string | null;
};

type Builder = Pick<Database, 'select'>;

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
