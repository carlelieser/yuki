import { describe, expect, it } from 'vitest';
import { ApkFetchError } from '../apk/http-source.ts';
import { ApkFormatError } from '../apk/format-error.ts';
import type { ApkIdentity } from '../apk/identity.ts';
import type { PendingAsset, Reclassification } from '../persistence/identities.ts';
import { identifyAssets, type IdentityPorts } from './identities.ts';

const signed: ApkIdentity = {
	packageName: 'com.acme.app',
	signers: ['aa'],
	lineage: []
};

function pending(id: string, listingId = 'listing-1'): PendingAsset {
	return { id, listingId, downloadUrl: `https://example.com/${id}.apk` };
}

type Recorded = {
	saved: Map<string, ApkIdentity | null>;
	settled: string[][];
	limits: number[];
	events: string[];
};

const UNCHANGED: Reclassification = { listingIds: [], foreignCount: 0, ownCount: 0 };

function ports(
	assets: PendingAsset[],
	read: IdentityPorts['read'],
	reclassification: Reclassification = UNCHANGED
): IdentityPorts & Recorded {
	const recorded: Recorded = { saved: new Map(), settled: [], limits: [], events: [] };

	return {
		...recorded,
		listPending: async (limit) => {
			recorded.limits.push(limit);
			return assets.slice(0, limit);
		},
		read,
		save: async (assetId, identity) => {
			recorded.events.push(`save ${assetId}`);
			recorded.saved.set(assetId, identity);
		},
		reclassify: async () => {
			recorded.events.push('reclassify');
			return reclassification;
		},
		settleListings: async (listingIds) => {
			recorded.events.push('settle');
			recorded.settled.push([...listingIds].sort());
		}
	};
}

describe('identifyAssets', () => {
	it('stores the identity read from each pending release', async () => {
		const run = ports([pending('v1'), pending('v2', 'listing-2')], async () => signed);

		const summary = await identifyAssets(run, { limit: 10, concurrency: 2 });

		expect([...run.saved.keys()].sort()).toEqual(['v1', 'v2']);
		expect(run.saved.get('v1')).toEqual(signed);
		expect(run.settled).toEqual([['listing-1', 'listing-2']]);
		expect(summary.identifiedCount).toBe(2);
	});

	it('asks for no more releases than the budget allows', async () => {
		const run = ports([pending('v1'), pending('v2'), pending('v3')], async () => signed);

		await identifyAssets(run, { limit: 2, concurrency: 8 });

		expect(run.limits).toEqual([2]);
		expect(run.saved.size).toBe(2);
	});

	it('leaves a release pending when the download could not be fetched', async () => {
		const run = ports([pending('v1')], async () => {
			throw new ApkFetchError('expected a partial response, got 503');
		});

		const summary = await identifyAssets(run, { limit: 10, concurrency: 1 });

		expect(run.saved.size).toBe(0);
		expect(summary.deferredCount).toBe(1);
		expect(summary.warnings).toHaveLength(1);
	});

	it('marks a malformed apk read and reports why', async () => {
		const run = ports([pending('v1')], async () => {
			throw new ApkFormatError('reading the v2 signers ran out of bounds');
		});

		const summary = await identifyAssets(run, { limit: 10, concurrency: 1 });

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

		await expect(identifyAssets(run, { limit: 10, concurrency: 1 })).rejects.toThrow('parser bug');
		expect(run.saved.size).toBe(0);
	});

	it('records an unsigned apk and still settles its listing', async () => {
		const run = ports([pending('v1')], async () => ({ ...signed, signers: [] }));

		const summary = await identifyAssets(run, { limit: 10, concurrency: 1 });

		expect(summary.unsignedCount).toBe(1);
		expect(summary.unreadableCount).toBe(0);
		expect(run.settled).toEqual([['listing-1']]);
	});

	it('records an apk it does not recognise and settles its listing', async () => {
		const run = ports([pending('v1')], async () => null);

		const summary = await identifyAssets(run, { limit: 10, concurrency: 1 });

		expect(run.saved.get('v1')).toBeNull();
		expect(summary.unreadableCount).toBe(1);
		expect(run.settled).toEqual([['listing-1']]);
	});

	it('classifies against the platform table only after saving new identities', async () => {
		const run = ports([pending('v1')], async () => signed);

		await identifyAssets(run, { limit: 10, concurrency: 1 });

		expect(run.events).toEqual(['save v1', 'reclassify', 'settle']);
	});

	it('settles listings whose apks changed classification even with nothing pending', async () => {
		const run = ports([], async () => signed, {
			listingIds: ['listing-7', 'listing-9'],
			foreignCount: 3,
			ownCount: 70
		});

		const summary = await identifyAssets(run, { limit: 10, concurrency: 1 });

		expect(run.settled).toEqual([['listing-7', 'listing-9']]);
		expect(summary.newlyForeignCount).toBe(3);
		expect(summary.newlyOwnCount).toBe(70);
	});

	it('settles a listing once when it was both read and reclassified', async () => {
		const run = ports([pending('v1')], async () => signed, {
			listingIds: ['listing-1'],
			foreignCount: 1,
			ownCount: 0
		});

		await identifyAssets(run, { limit: 10, concurrency: 1 });

		expect(run.settled).toEqual([['listing-1']]);
	});

	it('does not settle a listing whose only apk could not be fetched', async () => {
		const run = ports([pending('v1')], async () => {
			throw new ApkFetchError('requesting the apk failed');
		});

		await identifyAssets(run, { limit: 10, concurrency: 1 });

		expect(run.settled).toEqual([[]]);
	});
});
