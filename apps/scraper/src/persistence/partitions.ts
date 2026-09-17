import { eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

const CODE_PARTITION_TTL_MS = 30 * 24 * 60 * 60 * 1000;

export function isCodePartitionStale(
	partition: string,
	completedAt: Date,
	now: Date = new Date()
): boolean {
	if (!partition.startsWith('code:')) return false;
	return now.getTime() - completedAt.getTime() > CODE_PARTITION_TTL_MS;
}

export async function isPartitionComplete(db: Database, partition: string): Promise<boolean> {
	const [row] = await db
		.select({
			completedAt: schema.scrapePartitions.completedAt,
			cursorPage: schema.scrapePartitions.cursorPage
		})
		.from(schema.scrapePartitions)
		.where(eq(schema.scrapePartitions.partition, partition))
		.limit(1);

	if (row === undefined) return false;
	if (row.cursorPage !== null) return false;

	return !isCodePartitionStale(partition, row.completedAt);
}

export async function markPartitionComplete(db: Database, partition: string): Promise<void> {
	await db
		.insert(schema.scrapePartitions)
		.values({ partition, cursorPage: null, completedAt: new Date() })
		.onConflictDoUpdate({
			target: schema.scrapePartitions.partition,
			set: { cursorPage: null, completedAt: new Date() }
		});
}

export async function readPartitionCursor(db: Database, partition: string): Promise<number | null> {
	const [row] = await db
		.select({ cursorPage: schema.scrapePartitions.cursorPage })
		.from(schema.scrapePartitions)
		.where(eq(schema.scrapePartitions.partition, partition))
		.limit(1);

	return row?.cursorPage ?? null;
}

export async function writePartitionCursor(
	db: Database,
	partition: string,
	page: number
): Promise<void> {
	await db
		.insert(schema.scrapePartitions)
		.values({ partition, cursorPage: page, completedAt: new Date() })
		.onConflictDoUpdate({
			target: schema.scrapePartitions.partition,
			set: { cursorPage: page, completedAt: new Date() }
		});
}

export async function clearPartitionCursor(db: Database, partition: string): Promise<void> {
	await db
		.update(schema.scrapePartitions)
		.set({ cursorPage: null })
		.where(eq(schema.scrapePartitions.partition, partition));
}
