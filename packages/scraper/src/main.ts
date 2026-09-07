import { createDatabase } from '@yuki/db';
import { requireGithubToken } from './env.ts';
import { createGithubClient } from './github/client.ts';
import { listListingsForRefresh, touchListing, upsertListing } from './persistence/listings.ts';
import { finishRun, startRun } from './persistence/runs.ts';
import { readEtag, writeEtag } from './persistence/sources.ts';
import { runNightly } from './run/nightly.ts';

const DEFAULT_MAX_REPOS = 200;
const DEFAULT_MAX_REFRESH = 500;

function readNumberFlag(flag: string, fallback: number): number {
	const prefix = `${flag}=`;
	const argument = process.argv.find((value) => value.startsWith(prefix));
	if (argument === undefined) return fallback;

	const parsed = Number.parseInt(argument.slice(prefix.length), 10);
	return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

const shouldDiscover = process.argv.includes('--discover');
const maxRepos = readNumberFlag('--max-repos', DEFAULT_MAX_REPOS);
const maxRefresh = readNumberFlag('--max-refresh', DEFAULT_MAX_REFRESH);

const db = createDatabase();
const client = createGithubClient(requireGithubToken());
const runId = await startRun(db);

try {
	const summary = await runNightly(
		{
			client,
			etags: {
				read: (resource) => readEtag(db, resource),
				write: (resource, etag) => writeEtag(db, resource, etag)
			},
			listTargets: (limit) => listListingsForRefresh(db, limit),
			persist: (input) => upsertListing(db, input),
			touch: (listingId) => touchListing(db, listingId),
			log: (message) => console.log(message)
		},
		{ shouldDiscover, maxRepos, maxRefresh }
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
