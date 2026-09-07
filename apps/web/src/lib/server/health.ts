import { sql } from 'drizzle-orm';
import type { Database } from '@yuki/db';
import type { RedisClient } from '@yuki/redis';

export type DependencyStatus = { isHealthy: true } | { isHealthy: false; error: string };

export type HealthReport = {
	isHealthy: boolean;
	dependencies: Record<'postgres' | 'redis', DependencyStatus>;
};

async function probe(check: () => Promise<unknown>): Promise<DependencyStatus> {
	try {
		await check();
		return { isHealthy: true };
	} catch (cause) {
		return { isHealthy: false, error: cause instanceof Error ? cause.message : String(cause) };
	}
}

export async function checkHealth(db: Database, redis: RedisClient): Promise<HealthReport> {
	const [postgres, redisStatus] = await Promise.all([
		probe(() => db.execute(sql`select 1`)),
		probe(() => redis.ping())
	]);

	return {
		isHealthy: postgres.isHealthy && redisStatus.isHealthy,
		dependencies: { postgres, redis: redisStatus }
	};
}
