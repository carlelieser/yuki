import { createDatabase } from '@yuki/db';
import { createGithubClient, requireGithubToken } from '@yuki/github';
import { collectRepositoryEvidence } from './detection/reverify.ts';
import { scoreConfidence } from './detection/evidence.ts';
import { listListingsForReverify, replaceEvidence } from './persistence/listings.ts';
import { readListFlag } from './run/args.ts';

const DEFAULT_LIMIT = 5000;

function readNumberFlag(flag: string, fallback: number): number {
	const prefix = `${flag}=`;
	const argument = process.argv.find((value) => value.startsWith(prefix));
	if (argument === undefined) return fallback;

	const parsed = Number.parseInt(argument.slice(prefix.length), 10);
	return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

const apply = process.argv.includes('--apply');
const slugs = readListFlag(process.argv, '--slug');
const limit = readNumberFlag('--limit', DEFAULT_LIMIT);
const publishedOnly = process.argv.includes('--published-only');

const db = createDatabase();
const client = createGithubClient(requireGithubToken());

const candidates = await listListingsForReverify(db, limit, publishedOnly);
const targets =
	slugs.length > 0 ? candidates.filter((listing) => slugs.includes(listing.slug)) : candidates;

console.log(`Reverifying ${targets.length} listings${apply ? '' : ' (dry run)'}.`);

let strong = 0;
let belowStrong = 0;
let unpublished = 0;
let published = 0;
let inconclusive = 0;

for (const listing of targets) {
	const label = `${listing.owner}/${listing.name}`;

	let verdict;
	try {
		verdict = await collectRepositoryEvidence(client, listing.owner, listing.name);
	} catch (cause) {
		inconclusive += 1;
		const reason = cause instanceof Error ? cause.message : String(cause);
		console.warn(`  ? ${label}: ${reason}`);
		continue;
	}

	if (verdict.kind === 'inconclusive') {
		inconclusive += 1;
		console.warn(`  ? ${label}: ${verdict.reason}`);
		continue;
	}

	const confidence = scoreConfidence(verdict.evidence);
	const kinds = verdict.evidence.map((entry) => entry.kind).join(',') || '(none)';

	if (!apply) {
		console.log(`  ${confidence.padEnd(8)} ${label} [${kinds}]`);
		continue;
	}

	const outcome = await replaceEvidence(db, listing.id, verdict.evidence);

	if (outcome.confidence === 'strong') strong += 1;
	else belowStrong += 1;

	if (outcome.wasPublished && !outcome.isPublished) {
		unpublished += 1;
		console.log(`  unpublished ${label} (${outcome.confidence}) [${kinds}]`);
	}

	if (!outcome.wasPublished && outcome.isPublished) {
		published += 1;
		console.log(`  published ${label} (${outcome.confidence}) [${kinds}]`);
	}
}

if (!apply) {
	console.log(`\nDry run (${inconclusive} inconclusive). Re-run with --apply to write.`);
	process.exit(0);
}

console.log(
	`\nDone. strong ${strong}, below strong ${belowStrong}, published ${published}, ` +
		`unpublished ${unpublished}, inconclusive ${inconclusive} (left untouched).`
);
process.exit(0);
