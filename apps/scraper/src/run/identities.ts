import { ApkFetchError } from '../apk/http-source.ts';
import { ApkFormatError } from '../apk/format-error.ts';
import type { ApkIdentity } from '../apk/identity.ts';
import type { PendingAsset } from '../persistence/identities.ts';

export const DEFAULT_MAX_IDENTITIES = 3000;
export const IDENTITY_CONCURRENCY = 8;

export type IdentityPorts = {
	listPending: (limit: number) => Promise<PendingAsset[]>;
	read: (downloadUrl: string) => Promise<ApkIdentity | null>;
	save: (assetId: string, identity: ApkIdentity | null) => Promise<void>;
	settleListings: (listingIds: string[]) => Promise<void>;
};

export type IdentityOptions = {
	limit: number;
	concurrency: number;
};

export type IdentitySummary = {
	identifiedCount: number;
	foreignCount: number;
	unsignedCount: number;
	unreadableCount: number;
	deferredCount: number;
	warnings: string[];
};

type IdentityOutcome =
	| { kind: 'identified' | 'foreign' | 'unsigned' | 'unrecognised'; asset: PendingAsset }
	| { kind: 'malformed' | 'deferred'; asset: PendingAsset; reason: string };

export async function identifyAssets(
	ports: IdentityPorts,
	options: IdentityOptions
): Promise<IdentitySummary> {
	const pending = await ports.listPending(options.limit);
	const outcomes = await mapConcurrently(pending, options.concurrency, (asset) =>
		identify(ports, asset)
	);

	const saved = outcomes.filter((outcome) => outcome.kind !== 'deferred');
	await ports.settleListings([...new Set(saved.map((outcome) => outcome.asset.listingId))]);

	return summarize(outcomes);
}

async function identify(ports: IdentityPorts, asset: PendingAsset): Promise<IdentityOutcome> {
	let identity: ApkIdentity | null;

	try {
		identity = await ports.read(asset.downloadUrl);
	} catch (cause) {
		if (cause instanceof ApkFetchError) return { kind: 'deferred', asset, reason: cause.message };
		if (!(cause instanceof ApkFormatError)) throw cause;

		await ports.save(asset.id, null);
		return { kind: 'malformed', asset, reason: cause.message };
	}

	await ports.save(asset.id, identity);

	if (identity === null) return { kind: 'unrecognised', asset };
	return { kind: identityKind(identity), asset };
}

function identityKind(identity: ApkIdentity): 'identified' | 'foreign' | 'unsigned' {
	if (identity.signers.length === 0) return 'unsigned';
	return identity.isForeign ? 'foreign' : 'identified';
}

function summarize(outcomes: IdentityOutcome[]): IdentitySummary {
	const count = (...kinds: IdentityOutcome['kind'][]) =>
		outcomes.filter((outcome) => kinds.includes(outcome.kind)).length;

	return {
		identifiedCount: count('identified'),
		foreignCount: count('foreign'),
		unsignedCount: count('unsigned'),
		unreadableCount: count('unrecognised', 'malformed'),
		deferredCount: count('deferred'),
		warnings: outcomes.flatMap((outcome) =>
			'reason' in outcome ? [`${outcome.asset.downloadUrl}: ${outcome.reason}`] : []
		)
	};
}

async function mapConcurrently<Item, Result>(
	items: Item[],
	concurrency: number,
	transform: (item: Item) => Promise<Result>
): Promise<Result[]> {
	const results: Result[] = new Array(items.length);
	let next = 0;

	const worker = async () => {
		while (next < items.length) {
			const index = next;
			next += 1;
			results[index] = await transform(items[index]!);
		}
	};

	await Promise.all(Array.from({ length: Math.max(1, concurrency) }, worker));
	return results;
}
