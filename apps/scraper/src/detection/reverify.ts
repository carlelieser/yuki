import { mergeEvidence, type DetectedEvidence } from './evidence.ts';
import type { GithubClient, GithubTree } from '@yuki/github';

const MANIFEST_FILE = /(^|\/)AndroidManifest\.xml$/;
const BUILD_FILE = /(^|\/)(build\.gradle(\.kts)?|libs\.versions\.toml)$/;
const SOURCE_FILE = /\.(kt|java)$/;

const PROVIDER_MARKER = 'rikka.shizuku.ShizukuProvider';
const GRADLE_MARKER = 'dev.rikka.shizuku';
const LEGACY_GRADLE_MARKER = 'moe.shizuku.api';
const RUNTIME_MARKERS = [
	'Shizuku.pingBinder',
	'Shizuku.checkSelfPermission',
	'Shizuku.requestPermission',
	'Shizuku.newProcess',
	'Shizuku.addRequestPermissionResultListener'
];

const MAX_FILES_PER_KIND = 12;
const MAX_SOURCE_FILES = 25;

type Blob = { path: string; sha: string };

function blobsMatching(tree: GithubTree, pattern: RegExp, limit = MAX_FILES_PER_KIND): Blob[] {
	const matched: Blob[] = [];

	for (const entry of tree.tree) {
		if (matched.length >= limit) break;
		if (entry.type !== 'blob' || entry.sha === undefined) continue;
		if (!pattern.test(entry.path)) continue;

		matched.push({ path: entry.path, sha: entry.sha });
	}

	return matched;
}

const PRIVILEGE_HINTS = ['shizuku', 'sui', 'root', 'install', 'permission', 'service'];

function likelyShizukuSources(tree: GithubTree): Blob[] {
	const sources = blobsMatching(tree, SOURCE_FILE, Number.POSITIVE_INFINITY);

	return sources
		.map((blob) => ({ blob, rank: hintRank(blob.path) }))
		.filter((entry) => entry.rank !== -1)
		.sort((left, right) => left.rank - right.rank)
		.slice(0, MAX_SOURCE_FILES)
		.map((entry) => entry.blob);
}

function hintRank(path: string): number {
	const lowered = path.toLowerCase();
	return PRIVILEGE_HINTS.findIndex((hint) => lowered.includes(hint));
}

export async function collectRepositoryEvidence(
	client: GithubClient,
	owner: string,
	name: string,
	branch: string
): Promise<DetectedEvidence[]> {
	const tree = await client.getTree(owner, name, branch);
	if (!tree.isModified) return [];

	const found: DetectedEvidence[] = [];

	for (const blob of blobsMatching(tree.body, MANIFEST_FILE)) {
		const content = await readBlob(client, owner, name, blob.sha);
		if (content?.includes(PROVIDER_MARKER)) {
			found.push({ kind: 'provider_class', detail: `${PROVIDER_MARKER} in ${blob.path}` });
			break;
		}
	}

	for (const blob of blobsMatching(tree.body, BUILD_FILE)) {
		const content = await readBlob(client, owner, name, blob.sha);
		if (content === null) continue;

		if (content.includes(GRADLE_MARKER)) {
			found.push({ kind: 'gradle_dependency', detail: `${GRADLE_MARKER} in ${blob.path}` });
		}

		if (content.includes(LEGACY_GRADLE_MARKER)) {
			found.push({
				kind: 'legacy_gradle_dependency',
				detail: `${LEGACY_GRADLE_MARKER} in ${blob.path}`
			});
		}
	}

	for (const blob of likelyShizukuSources(tree.body)) {
		if (blob.path.toLowerCase().includes('shizuku')) {
			found.push({ kind: 'source_filename', detail: blob.path });
		}

		const content = await readBlob(client, owner, name, blob.sha);
		const marker = RUNTIME_MARKERS.find((candidate) => content?.includes(candidate));

		if (marker !== undefined) {
			found.push({ kind: 'runtime_api_call', detail: `${marker} in ${blob.path}` });
			break;
		}
	}

	return mergeEvidence(found);
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
