import { sql } from 'drizzle-orm';
import type { Database } from '@yuki/db';

export type DependencyStatus = { isHealthy: true } | { isHealthy: false; error: string };

export type HealthReport = {
	isHealthy: boolean;
	dependencies: Record<'postgres', DependencyStatus>;
};

async function probe(check: () => Promise<unknown>): Promise<DependencyStatus> {
	try {
		await check();
		return { isHealthy: true };
	} catch (cause) {
		return { isHealthy: false, error: cause instanceof Error ? cause.message : String(cause) };
	}
}

export async function checkHealth(db: Database): Promise<HealthReport> {
	const postgres = await probe(() => db.execute(sql`select 1`));

	return { isHealthy: postgres.isHealthy, dependencies: { postgres } };
}
