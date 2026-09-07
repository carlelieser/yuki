import {
	MAX_PAGES,
	RESULTS_PER_PAGE,
	buildCodeSearchQueries,
	buildRepoSearchQueries,
	isMarkdownPath,
	isSliceTruncated,
	isSourceFilenameMatch
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

export async function discover(client: GithubClient, maxRepos: number): Promise<DiscoveryResult> {
	const found = new Map<number, DiscoveredRepo>();
	const warnings: string[] = [];

	for (const query of buildCodeSearchQueries()) {
		if (found.size >= maxRepos) break;

		for (let page = 1; page <= MAX_PAGES; page += 1) {
			const response = await client.searchCode(query.q, page, RESULTS_PER_PAGE);
			if (!response.isModified) break;

			const { total_count: totalCount, items } = response.body;

			if (page === 1 && isSliceTruncated(totalCount)) {
				warnings.push(
					`Query "${query.q}" reports ${totalCount} results, beyond what paging reaches`
				);
			}

			for (const item of items) {
				if (isMarkdownPath(item.path)) continue;

				record(found, item.repository, { kind: query.evidence, detail: query.detail });

				if (isSourceFilenameMatch(item.path)) {
					record(found, item.repository, { kind: 'source_filename', detail: item.path });
				}
			}

			if (items.length < RESULTS_PER_PAGE) break;
			if (found.size >= maxRepos) break;
		}
	}

	for (const query of buildRepoSearchQueries()) {
		if (found.size >= maxRepos) break;

		for (let page = 1; page <= MAX_PAGES; page += 1) {
			const response = await client.searchRepositories(query.q, page, RESULTS_PER_PAGE);
			if (!response.isModified) break;

			for (const repo of response.body.items) {
				record(found, repo, { kind: query.evidence, detail: query.detail }, repo);
			}

			if (response.body.items.length < RESULTS_PER_PAGE) break;
			if (found.size >= maxRepos) break;
		}
	}

	return { repos: [...found.values()].slice(0, maxRepos), warnings };
}
