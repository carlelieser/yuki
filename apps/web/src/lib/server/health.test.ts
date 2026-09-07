import { describe, expect, it } from 'vitest';
import { checkHealth } from './health.ts';
import type { Database } from '@yuki/db';
import type { RedisClient } from '@yuki/redis';

const workingDb = { execute: async () => [{ '?column?': 1 }] } as unknown as Database;
const workingRedis = { ping: async () => 'PONG' } as unknown as RedisClient;

describe('checkHealth', () => {
	it('reports healthy when both dependencies respond', async () => {
		const report = await checkHealth(workingDb, workingRedis);

		expect(report.isHealthy).toBe(true);
		expect(report.dependencies.postgres.isHealthy).toBe(true);
		expect(report.dependencies.redis.isHealthy).toBe(true);
	});

	it('surfaces the postgres failure reason instead of hiding it', async () => {
		const failingDb = {
			execute: async () => {
				throw new Error('connection refused');
			}
		} as unknown as Database;

		const report = await checkHealth(failingDb, workingRedis);

		expect(report.isHealthy).toBe(false);
		expect(report.dependencies.postgres).toEqual({
			isHealthy: false,
			error: 'connection refused'
		});
		expect(report.dependencies.redis.isHealthy).toBe(true);
	});

	it('reports unhealthy when redis fails', async () => {
		const failingRedis = {
			ping: async () => {
				throw new Error('redis unreachable');
			}
		} as unknown as RedisClient;

		const report = await checkHealth(workingDb, failingRedis);

		expect(report.isHealthy).toBe(false);
		expect(report.dependencies.redis).toEqual({
			isHealthy: false,
			error: 'redis unreachable'
		});
	});
});
