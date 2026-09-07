import { eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

export type RunTotals = {
	discoveredCount: number;
	updatedCount: number;
	skippedCount: number;
	requestCount: number;
	notModifiedCount: number;
};

export async function startRun(db: Database): Promise<string> {
	const [row] = await db
		.insert(schema.scrapeRuns)
		.values({ status: 'running' })
		.returning({ id: schema.scrapeRuns.id });

	if (!row) throw new Error('Failed to open a scrape run');
	return row.id;
}

export async function finishRun(
	db: Database,
	runId: string,
	totals: RunTotals,
	error: string | null
): Promise<void> {
	await db
		.update(schema.scrapeRuns)
		.set({
			status: error === null ? 'succeeded' : 'failed',
			...totals,
			error,
			finishedAt: new Date()
		})
		.where(eq(schema.scrapeRuns.id, runId));
}
