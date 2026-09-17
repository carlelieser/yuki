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

const db = createDatabase();
const client = createGithubClient(requireGithubToken());

const candidates = await listListingsForReverify(db, limit);
const targets =
	slugs.length > 0 ? candidates.filter((listing) => slugs.includes(listing.slug)) : candidates;

console.log(`Reverifying ${targets.length} listings${apply ? '' : ' (dry run)'}.`);

let demoted = 0;
let promoted = 0;
let unpublished = 0;
let failed = 0;

for (const listing of targets) {
	const label = `${listing.owner}/${listing.name}`;

	let evidence;
	try {
		const repo = await client.getRepository(listing.owner, listing.name);
		if (!repo.isModified) {
			failed += 1;
			console.warn(`  ! ${label}: repository metadata unavailable`);
			continue;
		}

		evidence = await collectRepositoryEvidence(
			client,
			listing.owner,
			listing.name,
			repo.body.default_branch
		);
	} catch (cause) {
		failed += 1;
		const reason = cause instanceof Error ? cause.message : String(cause);
		console.warn(`  ! ${label}: ${reason}`);
		continue;
	}

	const confidence = scoreConfidence(evidence);
	const kinds = evidence.map((entry) => entry.kind).join(',') || '(none)';

	if (!apply) {
		console.log(`  ${confidence.padEnd(8)} ${label} [${kinds}]`);
		continue;
	}

	const outcome = await replaceEvidence(db, listing.id, evidence);

	if (outcome.confidence === 'strong') promoted += 1;
	else demoted += 1;

	if (outcome.wasPublished && !outcome.isPublished) {
		unpublished += 1;
		console.log(`  unpublished ${label} (${outcome.confidence}) [${kinds}]`);
	}
}

if (!apply) {
	console.log('\nDry run. Re-run with --apply to write these changes.');
	process.exit(0);
}

console.log(
	`\nDone. strong ${promoted}, below strong ${demoted}, unpublished ${unpublished}, failed ${failed}.`
);
process.exit(0);
