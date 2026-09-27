import { createDatabase } from '@yuki/db';
import { createGithubClient, GithubSkip, requireGithubToken } from '@yuki/github';
import {
	createCategorizer,
	fingerprintCategoryInput,
	type CategoryInput
} from './detection/category.ts';
import { localCategoryModel } from './detection/category-model.ts';
import { listListingsForCategorize, setListingCategory } from './persistence/listings.ts';

const DEFAULT_BUDGET_MINUTES = 300;

function readNumberFlag(flag: string, fallback: number): number {
	const prefix = `${flag}=`;
	const argument = process.argv.find((value) => value.startsWith(prefix));
	if (argument === undefined) return fallback;

	const parsed = Number.parseInt(argument.slice(prefix.length), 10);
	return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

const deadline = Date.now() + readNumberFlag('--budget-minutes', DEFAULT_BUDGET_MINUTES) * 60_000;

const db = createDatabase();
const client = createGithubClient(requireGithubToken());
const categorizer = createCategorizer(localCategoryModel());

async function readInput(owner: string, name: string): Promise<CategoryInput | null> {
	try {
		const repo = await client.getRepository(owner, name, null);
		if (!repo.isModified) return null;

		const readme = await client.getReadme(owner, name, null).catch((cause) => {
			if (cause instanceof GithubSkip) return null;
			throw cause;
		});

		return {
			description: repo.body.description,
			topics: repo.body.topics ?? [],
			readme: readme?.isModified ? readme.body : null
		};
	} catch (cause) {
		if (cause instanceof GithubSkip) return null;
		throw cause;
	}
}

const targets = await listListingsForCategorize(db);
console.log(`Checking ${targets.length} published listings.`);

let categorized = 0;
let unchanged = 0;
let failed = 0;

for (const target of targets) {
	if (Date.now() >= deadline) {
		console.log('Stopping at the time budget.');
		break;
	}

	const label = `${target.owner}/${target.name}`;

	try {
		const input = await readInput(target.owner, target.name);
		if (input === null) {
			failed += 1;
			console.warn(`  ? ${label}: repository unavailable`);
			continue;
		}

		const fingerprint = fingerprintCategoryInput(categorizer.modelId, input);
		if (fingerprint === target.categoryFingerprint) {
			unchanged += 1;
			continue;
		}

		const category = await categorizer.categorize(input);
		await setListingCategory(db, target.id, category, fingerprint);
		categorized += 1;
		console.log(`  ${category ?? '(none)'} ${label}`);
	} catch (cause) {
		failed += 1;
		const reason = cause instanceof Error ? cause.message : String(cause);
		console.warn(`  ? ${label}: ${reason}`);
	}
}

console.log(`Categorized ${categorized}, unchanged ${unchanged}, failed ${failed}.`);
process.exit(0);
