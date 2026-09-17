import { mergeEvidence, scoreConfidence, type DetectedEvidence } from './evidence.ts';
import { buildCodeSearchQueries, evidenceFromMatch, withRepository } from './queries.ts';
import type { GithubClient } from '@yuki/github';

const MATCHES_PER_QUERY = 5;

export type RepositoryVerdict =
	{ kind: 'verified'; evidence: DetectedEvidence[] } | { kind: 'inconclusive'; reason: string };

export async function collectRepositoryEvidence(
	client: GithubClient,
	owner: string,
	name: string
): Promise<RepositoryVerdict> {
	const found: DetectedEvidence[] = [];

	let sawAnyMatch = false;

	for (const query of buildCodeSearchQueries()) {
		const scoped = withRepository(query.q, owner, name);
		const response = await client.searchCode(scoped, 1, MATCHES_PER_QUERY);

		if (!response.isModified) {
			return { kind: 'inconclusive', reason: `"${query.q}" returned no readable response` };
		}

		if (response.body.items.length > 0) sawAnyMatch = true;

		for (const item of response.body.items) {
			found.push(...evidenceFromMatch(query, item.path));
		}

		if (scoreConfidence(found) === 'strong') break;
	}

	if (!sawAnyMatch) {
		return {
			kind: 'inconclusive',
			reason: 'no query matched anything, which an unindexed repository also looks like'
		};
	}

	return { kind: 'verified', evidence: mergeEvidence(found) };
}
