import { discover, type PartitionStore } from './discover.ts';
import { isSelfDeclaredOnly } from '../detection/evidence.ts';
import { GITHUB_EPOCH, type DateRange } from '../detection/queries.ts';
import { refreshListing, type EtagStore, type RefreshTarget } from './refresh.ts';
import type { GithubClient } from '@yuki/github';
import type { ListingRecord, PersistInput } from '../persistence/listings.ts';
import type { RunTotals } from '../persistence/runs.ts';

export type RunPorts = {
	client: GithubClient;
	etags: EtagStore;
	listTargets: (limit: number) => Promise<ListingRecord[]>;
	listKnownRepoIds?: () => Promise<number[]>;
	partitions?: PartitionStore;
	persist: (input: PersistInput) => Promise<string>;
	touch: (listingId: string) => Promise<void>;
	log?: (message: string) => void;
};

export type RunOptions = {
	shouldDiscover: boolean;
	maxRepos: number;
	maxRefresh: number;
	discoveryRange?: DateRange;
};

export type RunSummary = RunTotals & { warnings: string[] };

export async function runNightly(ports: RunPorts, options: RunOptions): Promise<RunSummary> {
	const log = ports.log ?? (() => {});
	const warnings: string[] = [];
	let discoveredCount = 0;
	let updatedCount = 0;
	let skippedCount = 0;

	const targets: RefreshTarget[] = [];
	const seenRepoIds = new Set<number>();

	if (options.shouldDiscover) {
		const knownRepoIds = new Set(await (ports.listKnownRepoIds?.() ?? Promise.resolve([])));
		const range = options.discoveryRange ?? { since: GITHUB_EPOCH, until: new Date() };

		const discovery = await discover(ports.client, (id) => knownRepoIds.has(id), {
			maxNewRepos: options.maxRepos,
			range,
			partitions: ports.partitions,
			log
		});
		warnings.push(...discovery.warnings);
		discoveredCount = discovery.repos.length;
		log(`Discovered ${discoveredCount} candidate repositories`);

		for (const found of discovery.repos) {
			seenRepoIds.add(found.githubRepoId);
			targets.push({
				owner: found.owner,
				name: found.name,
				evidence: found.evidence,
				repo: found.repo ?? undefined
			});
		}
	}

	const known = await ports.listTargets(options.maxRefresh);
	for (const listing of known) {
		if (seenRepoIds.has(listing.githubRepoId)) continue;
		targets.push({
			owner: listing.owner,
			name: listing.name,
			githubRepoId: listing.githubRepoId
		});
	}

	for (const target of targets) {
		const label = `${target.owner}/${target.name}`;

		try {
			const outcome = await refreshListing(ports.client, ports.etags, target);

			if (outcome.kind === 'skipped') {
				skippedCount += 1;
				await touchKnown(ports, known, target);
				log(`Skipped ${label}: ${outcome.reason}`);
				continue;
			}

			if (isUnprovenCandidate(outcome.input, seenRepoIds)) {
				skippedCount += 1;
				log(`Skipped ${label}: self-declared shizuku with no Android project`);
				continue;
			}

			try {
				await ports.persist(outcome.input);
			} catch (cause) {
				const reason = cause instanceof Error ? cause.message : String(cause);
				throw new Error(`persisting ${label} failed: ${reason}`, { cause });
			}

			updatedCount += 1;
			log(`Updated ${label}`);
		} catch (cause) {
			skippedCount += 1;
			const reason = cause instanceof Error ? cause.message : String(cause);
			warnings.push(`${label}: ${reason}`);
			log(`Failed ${label}: ${reason}`);
		}
	}

	return {
		discoveredCount,
		updatedCount,
		skippedCount,
		requestCount: ports.client.stats.requestCount,
		notModifiedCount: ports.client.stats.notModifiedCount,
		warnings
	};
}

function isUnprovenCandidate(input: PersistInput, discovered: Set<number>): boolean {
	if (input.listing === null) return false;
	if (!discovered.has(input.listing.githubRepoId)) return false;
	if (input.isAndroidApp !== false) return false;

	return isSelfDeclaredOnly(input.evidence);
}

async function touchKnown(
	ports: RunPorts,
	known: ListingRecord[],
	target: RefreshTarget
): Promise<void> {
	const match = known.find(
		(listing) => listing.owner === target.owner && listing.name === target.name
	);
	if (match !== undefined) {
		await ports.touch(match.id);
	}
}
