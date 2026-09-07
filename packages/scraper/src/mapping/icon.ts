import type { GithubTree } from '../github/types.ts';

const ICON_NAMES = ['ic_launcher.png', 'ic_launcher_round.png', 'ic_launcher_foreground.png'];
const DENSITY_ORDER = ['xxxhdpi', 'xxhdpi', 'xhdpi', 'hdpi', 'mdpi'];

function densityRank(path: string): number {
	const index = DENSITY_ORDER.findIndex((density) => path.includes(`mipmap-${density}`));
	return index === -1 ? DENSITY_ORDER.length : index;
}

export function findIconPath(tree: GithubTree): string | null {
	const candidates = tree.tree
		.filter((entry) => entry.type === 'blob')
		.map((entry) => entry.path)
		.filter((path) => {
			const lowered = path.toLowerCase();
			if (!lowered.includes('mipmap-')) return false;
			const filename = lowered.split('/').pop() ?? '';
			return ICON_NAMES.includes(filename);
		});

	if (candidates.length === 0) return null;

	return candidates.reduce((best, path) => (densityRank(path) < densityRank(best) ? path : best));
}

export function buildIconUrl(
	tree: GithubTree,
	owner: string,
	name: string,
	defaultBranch: string
): string | null {
	const path = findIconPath(tree);
	if (path === null) return null;

	return `https://raw.githubusercontent.com/${owner}/${name}/${defaultBranch}/${path}`;
}
