import { describe, expect, it } from 'vitest';
import { getPackageIndex, packageIndexQuery } from './package-index.ts';
import type { Database } from '@yuki/db';

function databaseReturning(rows: unknown[]): Database {
	const chain = {
		from: () => chain,
		where: () => chain,
		groupBy: () => chain,
		having: () => chain,
		innerJoin: () => chain,
		as: () => chain,
		orderBy: () => Promise.resolve(rows),
		then: (resolve: (value: unknown) => unknown) => Promise.resolve(rows).then(resolve)
	};

	return { select: () => chain } as unknown as Database;
}

describe('packageIndexQuery', () => {
	it('keeps only packages claimed by exactly one listing', () => {
		expect(packageIndexQuery().sql).toContain('having count(*) = 1');
	});

	it('groups the claim count by package name', () => {
		expect(packageIndexQuery().sql).toContain('group by "listings"."package_name"');
	});

	it('ignores listings that have no package name', () => {
		expect(packageIndexQuery().sql).toContain('"listings"."package_name" is not null');
	});

	it('serves published listings only', () => {
		expect(packageIndexQuery().sql).toContain('"listings"."is_published"');
	});
});

describe('getPackageIndex', () => {
	it('returns an entry for each listing the query matched', async () => {
		const db = databaseReturning([
			{
				packageName: 'dev.imranr.obtainium.fdroid',
				githubRepoId: 4242,
				slug: 'imranr98-obtainium',
				title: 'Obtainium',
				iconUrl: null
			}
		]);

		const index = await getPackageIndex(db);

		expect(index).toHaveLength(1);
		expect(index[0]?.packageName).toBe('dev.imranr.obtainium.fdroid');
		expect(index[0]?.githubRepoId).toBe(4242);
	});

	it('drops a row whose package name came back null', async () => {
		const db = databaseReturning([
			{ packageName: null, githubRepoId: 1, slug: 'a', title: 'A', iconUrl: null },
			{ packageName: 'com.termux', githubRepoId: 2, slug: 'termux', title: 'Termux', iconUrl: null }
		]);

		const index = await getPackageIndex(db);

		expect(index.map((entry) => entry.packageName)).toEqual(['com.termux']);
	});

	it('returns nothing when no listing has a package name', async () => {
		await expect(getPackageIndex(databaseReturning([]))).resolves.toEqual([]);
	});
});
