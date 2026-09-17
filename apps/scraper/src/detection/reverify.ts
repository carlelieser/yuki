import { mergeEvidence, type DetectedEvidence } from './evidence.ts';
import { describeMarker, filePatternFor, markersFor, type MarkerScope } from './markers.ts';
import { isSourceFilenameMatch } from './queries.ts';
import type { GithubClient, GithubTree } from '@yuki/github';

const MAX_FILES_PER_SCOPE: Record<MarkerScope, number> = {
	manifest: 12,
	build: 12,
	source: 25
};

const PRIVILEGE_HINTS = ['shizuku', 'sui', 'root', 'install', 'permission', 'service'];

type Blob = { path: string; sha: string };

export async function collectRepositoryEvidence(
	client: GithubClient,
	owner: string,
	name: string,
	branch: string
): Promise<DetectedEvidence[]> {
	const tree = await client.getTree(owner, name, branch);
	if (!tree.isModified) return [];

	const found: DetectedEvidence[] = [];

	for (const scope of ['manifest', 'build', 'source'] as const) {
		found.push(...(await scanScope(client, owner, name, tree.body, scope)));
	}

	return mergeEvidence(found);
}

async function scanScope(
	client: GithubClient,
	owner: string,
	name: string,
	tree: GithubTree,
	scope: MarkerScope
): Promise<DetectedEvidence[]> {
	const markers = markersFor(scope);
	const found: DetectedEvidence[] = [];
	const satisfied = new Set<string>();

	for (const blob of candidates(tree, scope)) {
		if (scope === 'source' && isSourceFilenameMatch(blob.path)) {
			found.push({ kind: 'source_filename', detail: blob.path });
		}

		if (satisfied.size === markers.length) continue;

		const content = await readBlob(client, owner, name, blob.sha);
		if (content === null) continue;

		for (const marker of markers) {
			if (satisfied.has(marker.literal)) continue;
			if (!content.includes(marker.literal)) continue;

			satisfied.add(marker.literal);
			found.push({ kind: marker.kind, detail: describeMarker(marker, blob.path) });
		}
	}

	return found;
}

function candidates(tree: GithubTree, scope: MarkerScope): Blob[] {
	const pattern = filePatternFor(scope);
	const matched: Blob[] = [];

	for (const entry of tree.tree) {
		if (entry.type !== 'blob' || entry.sha === undefined) continue;
		if (!pattern.test(entry.path)) continue;

		matched.push({ path: entry.path, sha: entry.sha });
	}

	if (scope !== 'source') return matched.slice(0, MAX_FILES_PER_SCOPE[scope]);

	return matched
		.map((blob) => ({ blob, rank: hintRank(blob.path) }))
		.filter((entry) => entry.rank !== -1)
		.sort((left, right) => left.rank - right.rank)
		.slice(0, MAX_FILES_PER_SCOPE.source)
		.map((entry) => entry.blob);
}

function hintRank(path: string): number {
	const lowered = path.toLowerCase();
	return PRIVILEGE_HINTS.findIndex((hint) => lowered.includes(hint));
}

async function readBlob(
	client: GithubClient,
	owner: string,
	name: string,
	sha: string
): Promise<string | null> {
	const response = await client.getBlob(owner, name, sha);
	return response.isModified ? response.body : null;
}
