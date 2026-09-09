import { describe, expect, it } from 'vitest';
import { discover, type PartitionStore } from './discover.ts';
import { GITHUB_EPOCH, RESULT_CAP, type DateRange } from '../detection/queries.ts';
import type { GithubClient } from '../github/client.ts';

type CodeItem = {
	path: string;
	repository: { id: number; name: string; owner: { login: string } };
};

function codeItem(id: number, path = 'app/src/main/AndroidManifest.xml'): CodeItem {
	return { path, repository: { id, name: `repo-${id}`, owner: { login: 'acme' } } };
}

function fakeClient(
	pages: (query: string, page: number) => { totalCount: number; items: CodeItem[] },
	seen: string[] = []
): GithubClient {
	return {
		stats: { requestCount: 0, notModifiedCount: 0 },
		searchCode: async (q: string, page: number) => {
			seen.push(q);
			const { totalCount, items } = pages(q, page);
			return { isModified: true, body: { total_count: totalCount, items } };
		},
		searchRepositories: async () => ({
			isModified: true,
			body: { total_count: 0, items: [] }
		})
	} as unknown as GithubClient;
}

const fullRange: DateRange = { since: GITHUB_EPOCH, until: new Date('2026-01-01T00:00:00.000Z') };

describe('discover', () => {
	it('skips repositories that are already indexed', async () => {
		const client = fakeClient(() => ({
			totalCount: 3,
			items: [codeItem(1), codeItem(2), codeItem(3)]
		}));

		const result = await discover(client, (id) => id === 1 || id === 2, {
			maxNewRepos: 10,
			range: fullRange
		});

		expect(result.repos.map((repo) => repo.githubRepoId)).toEqual([3]);
	});

	it('spends its budget on new repositories rather than known ones', async () => {
		const client = fakeClient(() => ({
			totalCount: 5,
			items: [codeItem(1), codeItem(2), codeItem(3), codeItem(4), codeItem(5)]
		}));

		const known = new Set([1, 2, 3]);
		const result = await discover(client, (id) => known.has(id), {
			maxNewRepos: 2,
			range: fullRange
		});

		expect(result.repos).toHaveLength(2);
		expect(result.repos.every((repo) => !known.has(repo.githubRepoId))).toBe(true);
	});

	it('subdivides a size band that overflows the paging cap', async () => {
		const queries: string[] = [];
		const client = fakeClient((q) => {
			const overflows = q.includes('size:>=0');
			return {
				totalCount: overflows ? RESULT_CAP + 1 : 1,
				items: overflows ? [] : [codeItem(42)]
			};
		}, queries);

		await discover(client, () => false, { maxNewRepos: 50, range: fullRange });

		expect(queries.some((q) => q.includes('size:0..'))).toBe(true);
		expect(queries.length).toBeGreaterThan(1);
	});

	it('never puts a date qualifier on a code search', async () => {
		const queries: string[] = [];
		const client = fakeClient(() => ({ totalCount: 1, items: [codeItem(1)] }), queries);

		await discover(client, () => false, { maxNewRepos: 5, range: fullRange });

		expect(queries.every((q) => !q.includes('created:'))).toBe(true);
	});

	it('does not record a partition that matched nothing', async () => {
		const completed: string[] = [];
		const client = fakeClient(() => ({ totalCount: 0, items: [] }));

		const result = await discover(client, () => false, {
			maxNewRepos: 10,
			range: fullRange,
			partitions: {
				isComplete: async () => false,
				markComplete: async (partition) => {
					completed.push(partition);
				}
			}
		});

		expect(completed).toEqual([]);
		expect(result.warnings.some((w) => w.includes('matched nothing'))).toBe(true);
	});

	it('does not revisit partitions already recorded as complete', async () => {
		const queries: string[] = [];
		const client = fakeClient(() => ({ totalCount: 1, items: [codeItem(7)] }), queries);

		const partitions: PartitionStore = {
			isComplete: async () => true,
			markComplete: async () => {}
		};

		const result = await discover(client, () => false, {
			maxNewRepos: 10,
			range: fullRange,
			partitions
		});

		expect(queries).toEqual([]);
		expect(result.repos).toEqual([]);
	});

	it('records a partition once it has been walked', async () => {
		const completed: string[] = [];
		const client = fakeClient(() => ({ totalCount: 1, items: [codeItem(9)] }));

		const partitions: PartitionStore = {
			isComplete: async () => false,
			markComplete: async (partition) => {
				completed.push(partition);
			}
		};

		await discover(client, () => false, { maxNewRepos: 10, range: fullRange, partitions });

		expect(completed.length).toBeGreaterThan(0);
	});
});
