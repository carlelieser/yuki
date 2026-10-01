import { createDatabase } from '@yuki/db';
import { settleListings } from './persistence/identities.ts';
import { listListingsBySlug, listListingsForRefresh } from './persistence/listings.ts';
import { findMissingSlugs, readListFlag } from './run/args.ts';
import { DEFAULT_MAX_IDENTITIES, IDENTITY_CONCURRENCY, identifyAssets } from './run/identities.ts';
import { createIdentityPorts } from './run/identity-ports.ts';

function readNumberFlag(flag: string, fallback: number): number {
	const prefix = `${flag}=`;
	const argument = process.argv.find((value) => value.startsWith(prefix));
	if (argument === undefined) return fallback;

	const parsed = Number.parseInt(argument.slice(prefix.length), 10);
	return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

const db = createDatabase();
const slugs = readListFlag(process.argv, '--slug');
const limit = readNumberFlag('--max-identities', DEFAULT_MAX_IDENTITIES);
const shouldSettleAll = process.argv.includes('--settle-all');

async function resolveListingIds(): Promise<string[] | undefined> {
	if (slugs.length === 0) return undefined;

	const listings = await listListingsBySlug(db, slugs);
	const missing = findMissingSlugs(
		slugs,
		listings.map((listing) => listing.slug)
	);
	if (missing.length > 0) throw new Error(`No listing with slug ${missing.join(', ')}`);

	return listings.map((listing) => listing.id);
}

const summary = await identifyAssets(createIdentityPorts(db, resolveListingIds), {
	limit,
	concurrency: IDENTITY_CONCURRENCY
});

for (const warning of summary.warnings) {
	console.warn(`Warning: ${warning}`);
}

console.log(
	`Identified ${summary.identifiedCount} release apks, ${summary.unsignedCount} unsigned, ` +
		`${summary.unreadableCount} unreadable, ${summary.deferredCount} deferred; ` +
		`${summary.newlyForeignCount} newly foreign, ${summary.newlyOwnCount} newly own`
);
if (shouldSettleAll) {
	const listingIds =
		(await resolveListingIds()) ?? (await listListingsForRefresh(db)).map((listing) => listing.id);
	await settleListings(db, listingIds);
	console.log(`Settled ${listingIds.length} listings`);
}

process.exit(0);
