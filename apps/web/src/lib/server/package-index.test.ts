import { describe, expect, it } from 'vitest';
import {
	buildIdentityIndex,
	getPackageIndex,
	packageIdentityQuery,
	packageIndexQuery,
	type IdentityRow
} from './package-index.ts';
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

function identityRow(overrides: Partial<IdentityRow> = {}): IdentityRow {
	return {
		packageName: 'dev.imranr.obtainium',
		githubRepoId: 4242,
		slug: 'imranr98-obtainium',
		title: 'Obtainium',
		iconUrl: null,
		signers: ['b353'],
		lineage: [],
		...overrides
	};
}

describe('packageIdentityQuery', () => {
	it('reads identities from published listings only', () => {
		expect(packageIdentityQuery().sql).toContain('"listings"."is_published"');
	});

	it('skips assets whose signers were never read', () => {
		expect(packageIdentityQuery().sql).toContain(
			'cardinality("listing_version_assets"."signer_digests") > 0'
		);
	});

	it('never advertises the signer of a foreign apk', () => {
		expect(packageIdentityQuery().sql).toContain('not "listing_version_assets"."is_foreign"');
	});
});

describe('buildIdentityIndex', () => {
	it('collapses the identities of every release into one entry per listing', () => {
		const index = buildIdentityIndex([
			identityRow(),
			identityRow(),
			identityRow({ signers: ['debug'] })
		]);

		expect(index).toEqual([
			expect.objectContaining({
				packageName: 'dev.imranr.obtainium',
				githubRepoId: 4242,
				identities: [
					{ signers: ['b353'], lineage: [] },
					{ signers: ['debug'], lineage: [] }
				]
			})
		]);
	});

	it('merges the rotation lineage recorded across releases', () => {
		const index = buildIdentityIndex([
			identityRow({ signers: ['new'], lineage: ['old'] }),
			identityRow({ signers: ['new'], lineage: ['older', 'old'] })
		]);

		expect(index[0]?.identities).toEqual([{ signers: ['new'], lineage: ['old', 'older'] }]);
	});

	it('tells apart two listings that share a package name but not a key', () => {
		const index = buildIdentityIndex([
			identityRow({ packageName: 'com.google.ar.core', githubRepoId: 1, signers: ['neko'] }),
			identityRow({ packageName: 'com.google.ar.core', githubRepoId: 2, signers: ['other'] })
		]);

		expect(index.map((entry) => [entry.githubRepoId, entry.identities[0]?.signers])).toEqual([
			[1, ['neko']],
			[2, ['other']]
		]);
	});

	it('drops an identity that two listings both claim', () => {
		const index = buildIdentityIndex([
			identityRow({ githubRepoId: 1, signers: ['shared'] }),
			identityRow({ githubRepoId: 2, signers: ['shared'] }),
			identityRow({ githubRepoId: 2, signers: ['own'] })
		]);

		expect(index).toEqual([
			expect.objectContaining({ githubRepoId: 2, identities: [{ signers: ['own'], lineage: [] }] })
		]);
	});

	it('treats a signer set as the same identity whatever its order', () => {
		const index = buildIdentityIndex([
			identityRow({ githubRepoId: 1, signers: ['a', 'b'] }),
			identityRow({ githubRepoId: 2, signers: ['b', 'a'] })
		]);

		expect(index).toEqual([]);
	});
});
