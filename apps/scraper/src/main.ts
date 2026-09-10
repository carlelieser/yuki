import { createDatabase } from '@yuki/db';
import { createGithubClient, requireGithubToken } from '@yuki/github';
import {
	listKnownRepoIds,
	listListingsForRefresh,
	touchListing,
	upsertListing
} from './persistence/listings.ts';
import { isPartitionComplete, markPartitionComplete } from './persistence/partitions.ts';
import { finishRun, lastSuccessfulRunAt, startRun } from './persistence/runs.ts';
import { GITHUB_EPOCH } from './detection/queries.ts';
import { readEtag, writeEtag } from './persistence/sources.ts';
import { runNightly } from './run/nightly.ts';

const DEFAULT_MAX_REPOS = 200;
const DEFAULT_SEED_MAX_REPOS = 10000;
const SEED_MAX_ATTEMPTS = 60;
const DEFAULT_MAX_REFRESH = 500;

function readNumberFlag(flag: string, fallback: number): number {
	const prefix = `${flag}=`;
	const argument = process.argv.find((value) => value.startsWith(prefix));
	if (argument === undefined) return fallback;

	const parsed = Number.parseInt(argument.slice(prefix.length), 10);
	return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

const shouldSeed = process.argv.includes('--seed');
const shouldDiscover = shouldSeed || process.argv.includes('--discover');
const maxRepos = readNumberFlag(
	'--max-repos',
	shouldSeed ? DEFAULT_SEED_MAX_REPOS : DEFAULT_MAX_REPOS
);
const maxRefresh = readNumberFlag('--max-refresh', DEFAULT_MAX_REFRESH);

const db = createDatabase();
const client = createGithubClient(
	requireGithubToken(),
	undefined,
	undefined,
	shouldSeed ? SEED_MAX_ATTEMPTS : undefined
);

const discoveryRange = shouldDiscover ? await resolveDiscoveryRange() : undefined;
const runId = await startRun(db);

async function resolveDiscoveryRange() {
	const until = new Date();
	if (shouldSeed) return { since: GITHUB_EPOCH, until };

	const watermark = await lastSuccessfulRunAt(db);
	return { since: watermark ?? GITHUB_EPOCH, until };
}

try {
	const summary = await runNightly(
		{
			client,
			etags: {
				read: (resource) => readEtag(db, resource),
				write: (resource, etag) => writeEtag(db, resource, etag)
			},
			listTargets: (limit) => listListingsForRefresh(db, limit),
			listKnownRepoIds: () => listKnownRepoIds(db),
			partitions: {
				isComplete: (partition) => isPartitionComplete(db, partition),
				markComplete: (partition) => markPartitionComplete(db, partition)
			},
			persist: (input) => upsertListing(db, input),
			touch: (listingId) => touchListing(db, listingId),
			log: (message) => console.log(message)
		},
		{ shouldDiscover, maxRepos, maxRefresh, discoveryRange }
	);

	const { warnings, ...totals } = summary;
	await finishRun(db, runId, totals, warnings.length === 0 ? null : warnings.join('\n'));

	console.log(
		`Run ${runId}: discovered ${totals.discoveredCount}, updated ${totals.updatedCount}, ` +
			`skipped ${totals.skippedCount}, requests ${totals.requestCount} ` +
			`(${totals.notModifiedCount} not modified)`
	);
	for (const warning of warnings) {
		console.warn(`Warning: ${warning}`);
	}

	process.exit(0);
} catch (cause) {
	const reason = cause instanceof Error ? cause.message : String(cause);
	await finishRun(
		db,
		runId,
		{
			discoveredCount: 0,
			updatedCount: 0,
			skippedCount: 0,
			requestCount: client.stats.requestCount,
			notModifiedCount: client.stats.notModifiedCount
		},
		reason
	);

	console.error(`Run ${runId} failed: ${reason}`);
	process.exit(1);
}
