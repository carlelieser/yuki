import type { GithubTree } from '@yuki/github';

const LFS_POINTER_MAX_BYTES = 200;
const LFS_POINTER_PREFIX = 'version https://git-lfs.github.com/spec/v1';
const MEDIA_EXTENSIONS = ['.png', '.webp', '.jpg', '.jpeg', '.gif', '.svg'];

export function isLfsPointer(content: string): boolean {
	return content.trimStart().startsWith(LFS_POINTER_PREFIX);
}

export async function collectLfsPaths(
	tree: GithubTree,
	readBlob: (sha: string) => Promise<string | null>
): Promise<Set<string>> {
	const paths = new Set<string>();

	for (const entry of tree.tree) {
		if (entry.type !== 'blob') continue;
		if (entry.sha === undefined || entry.size === undefined) continue;
		if (entry.size > LFS_POINTER_MAX_BYTES) continue;
		if (!MEDIA_EXTENSIONS.some((extension) => entry.path.toLowerCase().endsWith(extension))) {
			continue;
		}

		const content = await readBlob(entry.sha);
		if (content !== null && isLfsPointer(content)) paths.add(entry.path);
	}

	return paths;
}

export async function isLfsBlob(
	entry: { sha?: string; size?: number } | undefined,
	readBlob: (sha: string) => Promise<string | null>
): Promise<boolean> {
	if (entry?.sha === undefined || entry.size === undefined) return false;
	if (entry.size > LFS_POINTER_MAX_BYTES) return false;

	const content = await readBlob(entry.sha);
	return content !== null && isLfsPointer(content);
}

export function buildBlobUrl(
	owner: string,
	name: string,
	defaultBranch: string,
	path: string,
	isLfs: boolean
): string {
	const encoded = path
		.split('/')
		.map((segment) => encodeURIComponent(segment))
		.join('/');

	const host = isLfs
		? 'https://media.githubusercontent.com/media'
		: 'https://raw.githubusercontent.com';

	return `${host}/${owner}/${name}/${defaultBranch}/${encoded}`;
}
