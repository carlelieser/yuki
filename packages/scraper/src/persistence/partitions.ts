import { eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

export async function isPartitionComplete(db: Database, partition: string): Promise<boolean> {
	const [row] = await db
		.select({ id: schema.scrapePartitions.id })
		.from(schema.scrapePartitions)
		.where(eq(schema.scrapePartitions.partition, partition))
		.limit(1);

	return row !== undefined;
}

export async function markPartitionComplete(db: Database, partition: string): Promise<void> {
	await db
		.insert(schema.scrapePartitions)
		.values({ partition, completedAt: new Date() })
		.onConflictDoNothing();
}
