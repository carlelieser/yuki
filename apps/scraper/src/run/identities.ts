import { ApkFetchError } from '../apk/http-source.ts';
import { ApkFormatError } from '../apk/format-error.ts';
import type { ApkIdentity } from '../apk/identity.ts';
import type { PendingVersion } from '../persistence/identities.ts';

export const DEFAULT_MAX_IDENTITIES = 3000;
export const IDENTITY_CONCURRENCY = 8;

export type IdentityPorts = {
	listPending: (limit: number) => Promise<PendingVersion[]>;
	read: (downloadUrl: string) => Promise<ApkIdentity | null>;
	save: (versionId: string, identity: ApkIdentity | null) => Promise<void>;
	settlePackageNames: (listingIds: string[]) => Promise<void>;
};

export type IdentityOptions = {
	limit: number;
	concurrency: number;
};

export type IdentitySummary = {
	identifiedCount: number;
	unsignedCount: number;
	unreadableCount: number;
	deferredCount: number;
	warnings: string[];
};

type IdentityOutcome =
	| { kind: 'identified' | 'unsigned' | 'unrecognised'; version: PendingVersion }
	| { kind: 'malformed' | 'deferred'; version: PendingVersion; reason: string };

export async function identifyVersions(
	ports: IdentityPorts,
	options: IdentityOptions
): Promise<IdentitySummary> {
	const pending = await ports.listPending(options.limit);
	const outcomes = await mapConcurrently(pending, options.concurrency, (version) =>
		identify(ports, version)
	);

	const settled = outcomes.filter(
		(outcome) => outcome.kind === 'identified' || outcome.kind === 'unsigned'
	);
	await ports.settlePackageNames([...new Set(settled.map((outcome) => outcome.version.listingId))]);

	return summarize(outcomes);
}

async function identify(ports: IdentityPorts, version: PendingVersion): Promise<IdentityOutcome> {
	let identity: ApkIdentity | null;

	try {
		identity = await ports.read(version.downloadUrl);
	} catch (cause) {
		if (cause instanceof ApkFetchError) return { kind: 'deferred', version, reason: cause.message };
		if (!(cause instanceof ApkFormatError)) throw cause;

		await ports.save(version.id, null);
		return { kind: 'malformed', version, reason: cause.message };
	}

	await ports.save(version.id, identity);

	if (identity === null) return { kind: 'unrecognised', version };
	return { kind: identity.signers.length > 0 ? 'identified' : 'unsigned', version };
}

function summarize(outcomes: IdentityOutcome[]): IdentitySummary {
	const count = (...kinds: IdentityOutcome['kind'][]) =>
		outcomes.filter((outcome) => kinds.includes(outcome.kind)).length;

	return {
		identifiedCount: count('identified'),
		unsignedCount: count('unsigned'),
		unreadableCount: count('unrecognised', 'malformed'),
		deferredCount: count('deferred'),
		warnings: outcomes.flatMap((outcome) =>
			'reason' in outcome ? [`${outcome.version.downloadUrl}: ${outcome.reason}`] : []
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
