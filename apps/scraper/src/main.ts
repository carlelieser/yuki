import { createDatabase } from '@yuki/db';
import { createGithubClient, requireGithubToken } from '@yuki/github';
import { httpApkSource } from './apk/http-source.ts';
import { readApkPackageName } from './apk/package-name.ts';
import { requireIconConfig } from './icon/env.ts';
import { createApkIconExtractor } from './icon/extract.ts';
import { createR2Bucket } from './icon/r2-bucket.ts';
import { createIconStore } from './icon/store.ts';
import {
	listKnownRepoIds,
	listListingsBySlug,
	listListingsForRefresh,
	touchListing,
	upsertListing
} from './persistence/listings.ts';
import {
	clearPartitionCursor,
	isPartitionComplete,
	markPartitionComplete,
	readPartitionCursor,
	writePartitionCursor
} from './persistence/partitions.ts';
import { finishRun, lastSuccessfulRunAt, startRun } from './persistence/runs.ts';
import { GITHUB_EPOCH } from './detection/queries.ts';
import { readEtag, writeEtag } from './persistence/sources.ts';
import { runNightly } from './run/nightly.ts';
import { findMissingSlugs, readListFlag } from './run/args.ts';

const DEFAULT_MAX_REPOS = 200;
const DEFAULT_SEED_MAX_REPOS = 10000;

function readNumberFlag(flag: string, fallback: number): number {
	const prefix = `${flag}=`;
	const argument = process.argv.find((value) => value.startsWith(prefix));
	if (argument === undefined) return fallback;

	const parsed = Number.parseInt(argument.slice(prefix.length), 10);
	return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function readOptionalNumberFlag(flag: string): number | undefined {
	const prefix = `${flag}=`;
	const argument = process.argv.find((value) => value.startsWith(prefix));
	if (argument === undefined) return undefined;

	const parsed = Number.parseInt(argument.slice(prefix.length), 10);
	return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

const slugs = readListFlag(process.argv, '--slug');
const shouldSeed = process.argv.includes('--seed');
const shouldDiscover = shouldSeed || process.argv.includes('--discover');

if (slugs.length > 0 && shouldDiscover) {
	console.error('--slug cannot be combined with --discover or --seed');
	process.exit(1);
}

const maxRepos = readNumberFlag(
	'--max-repos',
	shouldSeed ? DEFAULT_SEED_MAX_REPOS : DEFAULT_MAX_REPOS
);
const maxRefresh = readOptionalNumberFlag('--max-refresh');

const db = createDatabase();
const client = createGithubClient(requireGithubToken());
const iconConfig = requireIconConfig();
const extractIcon = createApkIconExtractor(iconConfig.extractorBinary);
const icons = createIconStore(createR2Bucket(iconConfig.r2), iconConfig.assetsBaseUrl);

const discoveryRange = shouldDiscover ? await resolveDiscoveryRange() : undefined;
const runId = await startRun(db);

async function resolveSlugTargets() {
	const targets = await listListingsBySlug(db, slugs);
	const missing = findMissingSlugs(
		slugs,
		targets.map((target) => target.slug)
	);

	if (missing.length > 0) {
		throw new Error(`No listing with slug ${missing.join(', ')}`);
	}

	return targets;
}

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
			listTargets: (limit) =>
				slugs.length > 0 ? resolveSlugTargets() : listListingsForRefresh(db, limit),
			listKnownRepoIds: () => listKnownRepoIds(db),
			partitions: {
				isComplete: (partition) => isPartitionComplete(db, partition),
				markComplete: (partition) => markPartitionComplete(db, partition),
				readCursor: (partition) => readPartitionCursor(db, partition),
				writeCursor: (partition, page) => writePartitionCursor(db, partition, page),
				clearCursor: (partition) => clearPartitionCursor(db, partition)
			},
			persist: (input) => upsertListing(db, input),
			touch: (listingId) => touchListing(db, listingId),
			apk: {
				readPackage: (downloadUrl) => readApkPackageName(httpApkSource(downloadUrl)),
				resolveIcon: async (downloadUrl) => {
					const png = await extractIcon(downloadUrl);
					return png === null ? null : icons.publish(png);
				}
			},
			log: (message) => console.log(message)
		},
		{ shouldDiscover, maxRepos, maxRefresh, discoveryRange }
	);

	const { warnings, ...totals } = summary;
	await finishRun(
		db,
		runId,
		totals,
		warnings.length === 0 ? null : warnings.join('\n'),
		'succeeded'
	);

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
		reason,
		'failed'
	);

	console.error(`Run ${runId} failed: ${reason}`);
	process.exit(1);
}
