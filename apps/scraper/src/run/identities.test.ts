import { describe, expect, it } from 'vitest';
import { ApkFetchError } from '../apk/http-source.ts';
import { ApkFormatError } from '../apk/format-error.ts';
import type { ApkIdentity } from '../apk/identity.ts';
import type { PendingVersion } from '../persistence/identities.ts';
import { identifyVersions, type IdentityPorts } from './identities.ts';

const signed: ApkIdentity = {
	packageName: 'com.acme.app',
	signers: ['aa'],
	lineage: [],
	isForeign: false
};

function pending(id: string, listingId = 'listing-1'): PendingVersion {
	return { id, listingId, downloadUrl: `https://example.com/${id}.apk` };
}

type Recorded = {
	saved: Map<string, ApkIdentity | null>;
	settled: string[][];
	limits: number[];
};

function ports(versions: PendingVersion[], read: IdentityPorts['read']): IdentityPorts & Recorded {
	const recorded: Recorded = { saved: new Map(), settled: [], limits: [] };

	return {
		...recorded,
		listPending: async (limit) => {
			recorded.limits.push(limit);
			return versions.slice(0, limit);
		},
		read,
		save: async (versionId, identity) => {
			recorded.saved.set(versionId, identity);
		},
		settlePackageNames: async (listingIds) => {
			recorded.settled.push([...listingIds].sort());
		}
	};
}

describe('identifyVersions', () => {
	it('stores the identity read from each pending release', async () => {
		const run = ports([pending('v1'), pending('v2', 'listing-2')], async () => signed);

		const summary = await identifyVersions(run, { limit: 10, concurrency: 2 });

		expect([...run.saved.keys()].sort()).toEqual(['v1', 'v2']);
		expect(run.saved.get('v1')).toEqual(signed);
		expect(run.settled).toEqual([['listing-1', 'listing-2']]);
		expect(summary.identifiedCount).toBe(2);
	});

	it('asks for no more releases than the budget allows', async () => {
		const run = ports([pending('v1'), pending('v2'), pending('v3')], async () => signed);

		await identifyVersions(run, { limit: 2, concurrency: 8 });

		expect(run.limits).toEqual([2]);
		expect(run.saved.size).toBe(2);
	});

	it('leaves a release pending when the download could not be fetched', async () => {
		const run = ports([pending('v1')], async () => {
			throw new ApkFetchError('expected a partial response, got 503');
		});

		const summary = await identifyVersions(run, { limit: 10, concurrency: 1 });

		expect(run.saved.size).toBe(0);
		expect(summary.deferredCount).toBe(1);
		expect(summary.warnings).toHaveLength(1);
	});

	it('marks a malformed apk read and reports why', async () => {
		const run = ports([pending('v1')], async () => {
			throw new ApkFormatError('reading the v2 signers ran out of bounds');
		});

		const summary = await identifyVersions(run, { limit: 10, concurrency: 1 });

		expect(run.saved.has('v1')).toBe(true);
		expect(run.saved.get('v1')).toBeNull();
		expect(summary.unreadableCount).toBe(1);
		expect(summary.warnings).toEqual([
			'https://example.com/v1.apk: reading the v2 signers ran out of bounds'
		]);
	});

	it('lets an unexpected failure stop the run', async () => {
		const run = ports([pending('v1')], async () => {
			throw new TypeError('parser bug');
		});

		await expect(identifyVersions(run, { limit: 10, concurrency: 1 })).rejects.toThrow(
			'parser bug'
		);
		expect(run.saved.size).toBe(0);
	});

	it('records an unsigned apk and still settles its package name', async () => {
		const run = ports([pending('v1')], async () => ({ ...signed, signers: [] }));

		const summary = await identifyVersions(run, { limit: 10, concurrency: 1 });

		expect(summary.unsignedCount).toBe(1);
		expect(summary.unreadableCount).toBe(0);
		expect(run.settled).toEqual([['listing-1']]);
	});

	it('records an apk it does not recognise without settling a package name', async () => {
		const run = ports([pending('v1')], async () => null);

		const summary = await identifyVersions(run, { limit: 10, concurrency: 1 });

		expect(run.saved.get('v1')).toBeNull();
		expect(summary.unreadableCount).toBe(1);
		expect(run.settled).toEqual([[]]);
	});
});
