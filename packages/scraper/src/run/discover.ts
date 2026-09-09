import {
	MAX_PAGES,
	RESULTS_PER_PAGE,
	buildCodeSearchQueries,
	buildRepoSearchQueries,
	isIndivisible,
	isMarkdownPath,
	isSliceTruncated,
	isSourceFilenameMatch,
	splitRange,
	withCreatedRange,
	type DateRange
} from '../detection/queries.ts';
import type { DetectedEvidence } from '../detection/evidence.ts';
import type { GithubClient } from '../github/client.ts';
import type { GithubMinimalRepository, GithubRepository } from '../github/types.ts';

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
	isComplete: (query: string, range: DateRange) => Promise<boolean>;
	markComplete: (query: string, range: DateRange) => Promise<void>;
};

export type DiscoveryOptions = {
	maxNewRepos: number;
	range: DateRange;
	partitions?: PartitionStore;
	log?: (message: string) => void;
};

type SearchKind = 'code' | 'repository';

type PlannedQuery = {
	q: string;
	evidence: DetectedEvidence['kind'];
	detail: string;
	kind: SearchKind;
};

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

function plan(): PlannedQuery[] {
	return [
		...buildCodeSearchQueries().map((query) => ({ ...query, kind: 'code' as const })),
		...buildRepoSearchQueries().map((query) => ({ ...query, kind: 'repository' as const }))
	];
}

export async function discover(
	client: GithubClient,
	isKnown: (githubRepoId: number) => boolean,
	options: DiscoveryOptions
): Promise<DiscoveryResult> {
	const log = options.log ?? (() => {});
	const found = new Map<number, DiscoveredRepo>();
	const warnings: string[] = [];

	for (const query of plan()) {
		if (found.size >= options.maxNewRepos) break;

		await walk(query, options.range);
	}

	return { repos: [...found.values()].slice(0, options.maxNewRepos), warnings };

	async function walk(query: PlannedQuery, range: DateRange): Promise<void> {
		if (found.size >= options.maxNewRepos) return;

		const scoped = withCreatedRange(query.q, range);

		if (options.partitions !== undefined && (await options.partitions.isComplete(query.q, range))) {
			return;
		}

		const first = await search(query, scoped, 1);
		if (first === null) return;

		if (isSliceTruncated(first.totalCount)) {
			if (isIndivisible(range)) {
				warnings.push(
					`Query "${scoped}" reports ${first.totalCount} results in a single day, beyond what paging reaches`
				);
			} else {
				const [left, right] = splitRange(range);
				log(`Splitting "${query.q}" at ${first.totalCount} results`);
				await walk(query, left);
				await walk(query, right);
				return;
			}
		}

		absorb(query, first.items);

		for (let page = 2; page <= MAX_PAGES; page += 1) {
			if (first.items.length < RESULTS_PER_PAGE) break;
			if (found.size >= options.maxNewRepos) return;

			const next = await search(query, scoped, page);
			if (next === null) break;

			absorb(query, next.items);
			if (next.items.length < RESULTS_PER_PAGE) break;
		}

		await options.partitions?.markComplete(query.q, range);
	}

	async function search(
		query: PlannedQuery,
		scoped: string,
		page: number
	): Promise<{ totalCount: number; items: unknown[] } | null> {
		if (query.kind === 'code') {
			const response = await client.searchCode(scoped, page, RESULTS_PER_PAGE);
			if (!response.isModified) return null;
			return { totalCount: response.body.total_count, items: response.body.items };
		}

		const response = await client.searchRepositories(scoped, page, RESULTS_PER_PAGE);
		if (!response.isModified) return null;
		return { totalCount: response.body.total_count, items: response.body.items };
	}

	function absorb(query: PlannedQuery, items: unknown[]): void {
		for (const item of items) {
			if (query.kind === 'code') {
				const entry = item as { path: string; repository: GithubMinimalRepository };
				if (isMarkdownPath(entry.path)) continue;
				if (isKnown(entry.repository.id)) continue;

				record(found, entry.repository, { kind: query.evidence, detail: query.detail });

				if (isSourceFilenameMatch(entry.path)) {
					record(found, entry.repository, { kind: 'source_filename', detail: entry.path });
				}
				continue;
			}

			const repo = item as GithubRepository;
			if (isKnown(repo.id)) continue;
			record(found, repo, { kind: query.evidence, detail: query.detail }, repo);
		}
	}
}
