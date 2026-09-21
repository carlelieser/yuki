import type { GithubTree } from '@yuki/github';
import { blobs, findAdaptiveIconPath, splitPath } from './icon.ts';
import { parseAdaptiveIcon, parseColors, vectorToSvg } from './vector-icon.ts';
import { composeAdaptiveSvg, toDataUri } from './adaptive-svg.ts';

function qualifierRank(loweredPath: string, loweredResourceDir: string): number | null {
	const rest = loweredPath.slice(`${loweredResourceDir}/`.length);
	const dir = rest.split('/')[0] ?? '';
	const qualifiers = dir.split('-').slice(1);

	if (qualifiers.some((qualifier) => /^v\d+$/.test(qualifier))) return null;

	if (qualifiers.length === 0) return 2;
	if (qualifiers.length === 1 && qualifiers[0] === 'night') return 1;

	return null;
}

const COLOR_VALUES_FILE =
	/\/[^/]*colou?rs?[^/]*\.xml$|\/[^/]*ic_launcher[^/]*background[^/]*\.xml$/;

function resourceDirOf(adaptivePath: string): string {
	const marker = adaptivePath.search(/\/(mipmap|drawable)[^/]*\//);
	return marker === -1 ? '' : adaptivePath.slice(0, marker);
}

export async function readColorResources(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	resourceDir: string
): Promise<Map<string, string>> {
	const buckets: { rank: number; path: string }[] = [];
	for (const path of blobs(tree)) {
		const lowered = path.toLowerCase();
		if (!lowered.startsWith(`${resourceDir.toLowerCase()}/values`)) continue;
		if (!COLOR_VALUES_FILE.test(lowered)) continue;

		const rank = qualifierRank(lowered, resourceDir.toLowerCase());
		if (rank === null) continue;
		buckets.push({ rank, path });
	}

	buckets.sort((left, right) => right.rank - left.rank || right.path.localeCompare(left.path));

	const colors = new Map<string, string>();
	for (const { path } of buckets) {
		const xml = await read(path);
		if (xml === null) continue;
		for (const [key, value] of parseColors(xml)) colors.set(key, value);
	}

	return colors;
}

export function resourceDirFor(adaptivePath: string): string {
	return resourceDirOf(adaptivePath);
}

export async function buildVectorIcon(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	declaredPath: string | null = null
): Promise<string | null> {
	const adaptivePath = declaredPath ?? findAdaptiveIconPath(tree);
	if (adaptivePath === null) return null;

	const adaptiveXml = await read(adaptivePath);
	if (adaptiveXml === null) return null;

	const refs = parseAdaptiveIcon(adaptiveXml);
	const isPlainVector = refs.foreground === null && /<vector\b/.test(adaptiveXml);
	if (refs.foreground === null && !isPlainVector) return null;

	const resourceDir = resourceDirOf(adaptivePath);
	const available = new Set(blobs(tree));

	const colors = await readColorResources(tree, read, resourceDir);

	const colorResources: { rank: number; path: string }[] = [];
	for (const path of blobs(tree)) {
		const lowered = path.toLowerCase();
		if (!lowered.startsWith(`${resourceDir.toLowerCase()}/color`)) continue;
		if (!lowered.endsWith('.xml')) continue;

		const directory = splitPath(lowered).dir.split('/').pop() ?? '';
		if (!/^color(-|$)/.test(directory)) continue;

		const rank = qualifierRank(lowered, resourceDir.toLowerCase());
		if (rank === null) continue;
		colorResources.push({ rank, path });
	}

	colorResources.sort(
		(left, right) => right.rank - left.rank || right.path.localeCompare(left.path)
	);

	const gradients = new Map<string, string>();
	for (const { path } of colorResources) {
		const xml = await read(path);
		if (xml === null || !/<gradient\b/.test(xml)) continue;
		gradients.set(splitPath(path).stem, xml);
	}

	const readDrawable = async (drawable: string): Promise<string | null> => {
		const prefix = `${resourceDir}/drawable`;
		const candidates = [...available].filter((path) => {
			if (!path.startsWith(prefix)) return false;

			const { dir, filename, stem } = splitPath(path);
			if (stem !== drawable || !filename.endsWith('.xml')) return false;

			return /^drawable(-|$)/.test(dir.split('/').pop() ?? '');
		});

		candidates.sort((left, right) => left.length - right.length || left.localeCompare(right));

		for (const candidate of candidates) {
			const xml = await read(candidate);
			if (xml !== null) return xml;
		}

		return null;
	};

	if (refs.foreground === null) {
		const svg = vectorToSvg(adaptiveXml, colors, { gradients });
		return svg === null ? null : toDataUri(svg);
	}

	const foregroundXml =
		refs.foreground.kind === 'drawable' ? await readDrawable(refs.foreground.name) : null;
	if (foregroundXml === null) return null;

	let background: { kind: 'color' | 'vector'; value: string } | null = null;
	if (refs.background !== null) {
		if (refs.background.kind === 'color') {
			const reference = refs.background.name.startsWith('android:')
				? `@android:color/${refs.background.name.slice('android:'.length)}`
				: `@color/${refs.background.name}`;
			background = { kind: 'color', value: reference };
		} else {
			const backgroundXml = await readDrawable(refs.background.name);
			if (backgroundXml !== null) background = { kind: 'vector', value: backgroundXml };
		}
	}

	const svg = composeAdaptiveSvg({ background, foreground: foregroundXml, colors, gradients });
	return svg === null ? null : toDataUri(svg);
}
