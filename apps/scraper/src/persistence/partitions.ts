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
		.select({ completedAt: schema.scrapePartitions.completedAt })
		.from(schema.scrapePartitions)
		.where(eq(schema.scrapePartitions.partition, partition))
		.limit(1);

	if (row === undefined) return false;

	return !isCodePartitionStale(partition, row.completedAt);
}

export async function markPartitionComplete(db: Database, partition: string): Promise<void> {
	await db
		.insert(schema.scrapePartitions)
		.values({ partition, completedAt: new Date() })
		.onConflictDoUpdate({
			target: schema.scrapePartitions.partition,
			set: { completedAt: new Date() }
		});
}
