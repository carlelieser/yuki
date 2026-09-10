import {
	FULL_SIZE_RANGE,
	MAX_PAGES,
	RESULTS_PER_PAGE,
	buildCodeSearchQueries,
	buildRepoSearchQueries,
	describeSizeRange,
	isIndivisible,
	isMarkdownPath,
	isSizeIndivisible,
	isSliceTruncated,
	isSourceFilenameMatch,
	splitRange,
	splitSizeRange,
	withCreatedRange,
	withSizeRange,
	type DateRange,
	type SizeRange
} from '../detection/queries.ts';
import type { DetectedEvidence } from '../detection/evidence.ts';
import type { GithubClient } from '@yuki/github';
import type { GithubMinimalRepository, GithubRepository } from '@yuki/github';

export type DiscoveredRepo = {
	owner: string;
	name: string;
	githubRepoId: number;
	repo: GithubRepository | null;
	evidence: DetectedEvidence[];
};

export type DiscoveryResult = {
	repos: DiscoveredRepo[];
	warnings: string[];
};

export type PartitionStore = {
	isComplete: (partition: string) => Promise<boolean>;
	markComplete: (partition: string) => Promise<void>;
};

export type DiscoveryOptions = {
	maxNewRepos: number;
	range: DateRange;
	partitions?: PartitionStore;
	log?: (message: string) => void;
};

type CodeQuery = { q: string; evidence: DetectedEvidence['kind']; detail: string };

const MAX_SPLIT_DEPTH = 12;

function record(
	found: Map<number, DiscoveredRepo>,
	repo: GithubMinimalRepository,
	evidence: DetectedEvidence,
	full: GithubRepository | null = null
): void {
	const existing = found.get(repo.id);
	if (existing === undefined) {
		found.set(repo.id, {
			owner: repo.owner.login,
			name: repo.name,
			githubRepoId: repo.id,
			repo: full,
			evidence: [evidence]
		});
		return;
	}

	existing.evidence.push(evidence);
	existing.repo ??= full;
}

export async function discover(
	client: GithubClient,
	isKnown: (githubRepoId: number) => boolean,
	options: DiscoveryOptions
): Promise<DiscoveryResult> {
	const log = options.log ?? (() => {});
	const found = new Map<number, DiscoveredRepo>();
	const warnings: string[] = [];

	for (const query of buildCodeSearchQueries()) {
		if (found.size >= options.maxNewRepos) break;
		await walkCode(query, FULL_SIZE_RANGE);
	}

	for (const query of buildRepoSearchQueries()) {
		if (found.size >= options.maxNewRepos) break;
		await walkRepositories(query, options.range);
	}

	return { repos: [...found.values()].slice(0, options.maxNewRepos), warnings };

	async function walkCode(query: CodeQuery, size: SizeRange, depth = 0): Promise<void> {
		if (found.size >= options.maxNewRepos) return;

		if (depth > MAX_SPLIT_DEPTH) {
			warnings.push(
				`Query "${query.q}" still overflows after ${MAX_SPLIT_DEPTH} splits; giving up on ${describeSizeRange(size)}`
			);
			return;
		}

		const key = `code:${query.q}:${describeSizeRange(size)}`;
		if (options.partitions !== undefined && (await options.partitions.isComplete(key))) return;

		const scoped = withSizeRange(query.q, size);
		const first = await client.searchCode(scoped, 1, RESULTS_PER_PAGE);
		if (!first.isModified) return;

		const totalCount = first.body.total_count;

		if (totalCount === 0) {
			warnings.push(`Query "${scoped}" matched nothing; not recording it as searched`);
			return;
		}

		if (isSliceTruncated(totalCount)) {
			if (isSizeIndivisible(size)) {
				warnings.push(
					`Query "${scoped}" reports ${totalCount} results in the narrowest size band, beyond what paging reaches`
				);
			} else {
				const [lower, upper] = splitSizeRange(size);
				log(`Splitting "${query.q}" at ${totalCount} results (${describeSizeRange(size)})`);
				await walkCode(query, lower, depth + 1);
				await walkCode(query, upper, depth + 1);
				return;
			}
		}

		absorbCode(query, first.body.items);
		let lastPageSize = first.body.items.length;

		for (let page = 2; page <= MAX_PAGES && lastPageSize === RESULTS_PER_PAGE; page += 1) {
			if (found.size >= options.maxNewRepos) return;

			const next = await client.searchCode(scoped, page, RESULTS_PER_PAGE);
			if (!next.isModified) break;

			absorbCode(query, next.body.items);
			lastPageSize = next.body.items.length;
		}

		await options.partitions?.markComplete(key);
	}

	async function walkRepositories(
		query: { q: string; evidence: DetectedEvidence['kind']; detail: string },
		range: DateRange
	): Promise<void> {
		if (found.size >= options.maxNewRepos) return;

		const key = `repo:${query.q}:${range.since.toISOString()}..${range.until.toISOString()}`;
		if (options.partitions !== undefined && (await options.partitions.isComplete(key))) return;

		const scoped = withCreatedRange(query.q, range);
		const first = await client.searchRepositories(scoped, 1, RESULTS_PER_PAGE);
		if (!first.isModified) return;

		const totalCount = first.body.total_count;

		if (totalCount === 0) {
			warnings.push(`Query "${scoped}" matched nothing; not recording it as searched`);
			return;
		}

		if (isSliceTruncated(totalCount)) {
			if (isIndivisible(range)) {
				warnings.push(
					`Query "${scoped}" reports ${totalCount} results in a single day, beyond what paging reaches`
				);
			} else {
				const [left, right] = splitRange(range);
				log(`Splitting "${query.q}" at ${totalCount} results`);
				await walkRepositories(query, left);
				await walkRepositories(query, right);
				return;
			}
		}

		absorbRepositories(query, first.body.items);
		let lastPageSize = first.body.items.length;

		for (let page = 2; page <= MAX_PAGES && lastPageSize === RESULTS_PER_PAGE; page += 1) {
			if (found.size >= options.maxNewRepos) return;

			const next = await client.searchRepositories(scoped, page, RESULTS_PER_PAGE);
			if (!next.isModified) break;

			absorbRepositories(query, next.body.items);
			lastPageSize = next.body.items.length;
		}

		await options.partitions?.markComplete(key);
	}

	function absorbCode(
		query: CodeQuery,
		items: { path: string; repository: GithubMinimalRepository }[]
	): void {
		for (const item of items) {
			if (isMarkdownPath(item.path)) continue;
			if (isKnown(item.repository.id)) continue;

			record(found, item.repository, { kind: query.evidence, detail: query.detail });

			if (isSourceFilenameMatch(item.path)) {
				record(found, item.repository, { kind: 'source_filename', detail: item.path });
			}
		}
	}

	function absorbRepositories(
		query: { evidence: DetectedEvidence['kind']; detail: string },
		items: GithubRepository[]
	): void {
		for (const repo of items) {
			if (isKnown(repo.id)) continue;
			record(found, repo, { kind: query.evidence, detail: query.detail }, repo);
		}
	}
}
