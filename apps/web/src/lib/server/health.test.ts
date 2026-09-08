import { describe, expect, it } from 'vitest';
import { checkHealth } from './health.ts';
import type { Database } from '@yuki/db';

const workingDb = { execute: async () => [{ '?column?': 1 }] } as unknown as Database;

describe('checkHealth', () => {
	it('reports healthy when postgres responds', async () => {
		const report = await checkHealth(workingDb);

		expect(report.isHealthy).toBe(true);
		expect(report.dependencies.postgres.isHealthy).toBe(true);
	});

	it('surfaces the postgres failure reason instead of hiding it', async () => {
		const failingDb = {
			execute: async () => {
				throw new Error('connection refused');
			}
		} as unknown as Database;

		const report = await checkHealth(failingDb);

		expect(report.isHealthy).toBe(false);
		expect(report.dependencies.postgres).toEqual({
			isHealthy: false,
			error: 'connection refused'
		});
	});
});
