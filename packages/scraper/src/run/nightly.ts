import { discover } from './discover.ts';
import { refreshListing, type EtagStore, type RefreshTarget } from './refresh.ts';
import type { GithubClient } from '../github/client.ts';
import type { ListingRecord, PersistInput } from '../persistence/listings.ts';
import type { RunTotals } from '../persistence/runs.ts';

export type RunPorts = {
	client: GithubClient;
	etags: EtagStore;
	listTargets: (limit: number) => Promise<ListingRecord[]>;
	persist: (input: PersistInput) => Promise<string>;
	touch: (listingId: string) => Promise<void>;
	log?: (message: string) => void;
};

export type RunOptions = {
	shouldDiscover: boolean;
	maxRepos: number;
	maxRefresh: number;
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
		const discovery = await discover(ports.client, options.maxRepos);
		warnings.push(...discovery.warnings);
		discoveredCount = discovery.repos.length;
		log(`Discovered ${discoveredCount} candidate repositories`);

		for (const found of discovery.repos) {
			seenRepoIds.add(found.repo.id);
			targets.push({
				owner: found.repo.owner.login,
				name: found.repo.name,
				evidence: found.evidence,
				repo: found.repo
			});
		}
	}

	const known = await ports.listTargets(options.maxRefresh);
	for (const listing of known) {
		if (seenRepoIds.has(listing.githubRepoId)) continue;
		targets.push({ owner: listing.owner, name: listing.name });
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

			await ports.persist(outcome.input);
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
