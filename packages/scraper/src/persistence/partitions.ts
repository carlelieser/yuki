import { and, eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import type { DateRange } from '../detection/queries.ts';

export async function isPartitionComplete(
	db: Database,
	query: string,
	range: DateRange
): Promise<boolean> {
	const [row] = await db
		.select({ id: schema.scrapePartitions.id })
		.from(schema.scrapePartitions)
		.where(
			and(
				eq(schema.scrapePartitions.query, query),
				eq(schema.scrapePartitions.since, range.since),
				eq(schema.scrapePartitions.until, range.until)
			)
		)
		.limit(1);

	return row !== undefined;
}

export async function markPartitionComplete(
	db: Database,
	query: string,
	range: DateRange
): Promise<void> {
	await db
		.insert(schema.scrapePartitions)
		.values({ query, since: range.since, until: range.until, completedAt: new Date() })
		.onConflictDoNothing();
}
