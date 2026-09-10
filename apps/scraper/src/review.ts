import { createDatabase } from '@yuki/db';
import { listCandidates, setPublished } from './persistence/review.ts';
import { formatCandidateList, formatHelp, parseArgs } from './review/format.ts';

const command = parseArgs(process.argv.slice(2));

if (command.kind === 'help') {
	console.log(formatHelp());
	process.exit(0);
}

if (command.kind === 'invalid') {
	console.error(command.reason);
	console.error('');
	console.error(formatHelp());
	process.exit(1);
}

const db = createDatabase();

if (command.kind === 'pending' || command.kind === 'published') {
	const isPublished = command.kind === 'published';
	const candidates = await listCandidates(db, isPublished, command.limit);

	console.log(
		formatCandidateList(
			candidates,
			isPublished ? 'No listings are published yet.' : 'Nothing is waiting for review.'
		)
	);
	process.exit(0);
}

const isPublishing = command.kind === 'publish';
let failed = false;

for (const slug of command.slugs) {
	const found = await setPublished(db, slug, isPublishing);

	if (found) {
		console.log(`${isPublishing ? 'Published' : 'Unpublished'} ${slug}`);
	} else {
		console.error(`No listing with slug "${slug}"`);
		failed = true;
	}
}

process.exit(failed ? 1 : 0);
