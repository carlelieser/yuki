import type { GithubTree } from '@yuki/github';
import { blobs, findAdaptiveIconPath, splitPath } from './icon.ts';
import { parseAdaptiveIcon, parseColors, vectorToSvg } from './vector-icon.ts';
import { readShapeFill } from './solid-layer.ts';
import { composeAdaptiveSvg, toDataUri } from './adaptive-svg.ts';
import { drawableKindOf, readLayerItems } from './drawable-kind.ts';
import { createFidelity, markUnresolved, type Fidelity } from './fidelity.ts';

export type VectorIconResult = { svg: string; fidelity: Fidelity };

function qualifierRank(loweredPath: string, loweredResourceDir: string): number | null {
	const rest = loweredPath.slice(`${loweredResourceDir}/`.length);
	const dir = rest.split('/')[0] ?? '';
	const qualifiers = dir.split('-').slice(1);

	if (qualifiers.some((qualifier) => /^v\d+$/.test(qualifier))) return null;

	if (qualifiers.length === 0) return 2;
	if (qualifiers.length === 1 && qualifiers[0] === 'night') return 1;

	return null;
}

const SOURCE_SET_BONUS = 10;

const MAX_LAYER_DEPTH = 4;

const VECTOR_OPEN = /<vector\b[^>]*>/;

function viewportOf(vector: string): { width: number; height: number } | null {
	const header = vector.match(VECTOR_OPEN)?.[0];
	if (header === undefined) return null;

	const width = Number.parseFloat(header.match(/android:viewportWidth="([\d.]+)"/)?.[1] ?? '');
	const height = Number.parseFloat(header.match(/android:viewportHeight="([\d.]+)"/)?.[1] ?? '');
	if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) return null;

	return { width, height };
}

function mergeVectors(layers: string[]): string | null {
	const base = layers[0];
	if (base === undefined) return null;

	const target = viewportOf(base);
	if (target === null) return null;

	const bodies: string[] = [];
	for (const layer of layers) {
		const header = layer.match(VECTOR_OPEN)?.[0];
		if (header === undefined) continue;

		const inner = layer.slice(layer.indexOf(header) + header.length).replace(/<\/vector>\s*$/, '');
		if (inner.trim() === '') continue;

		const viewport = viewportOf(layer);
		if (viewport === null) continue;

		const scaleX = target.width / viewport.width;
		const scaleY = target.height / viewport.height;
		bodies.push(
			scaleX === 1 && scaleY === 1
				? inner
				: `<group android:scaleX="${scaleX}" android:scaleY="${scaleY}">${inner}</group>`
		);
	}

	if (bodies.length === 0) return null;

	return `<vector xmlns:android="http://schemas.android.com/apk/res/android" android:viewportWidth="${target.width}" android:viewportHeight="${target.height}">${bodies.join('')}</vector>`;
}

const COLOR_VALUES_FILE =
	/\/[^/]*colou?rs?[^/]*\.xml$|\/[^/]*ic_launcher[^/]*background[^/]*\.xml$/;

function resourceDirOf(adaptivePath: string): string {
	const marker = adaptivePath.search(/\/(mipmap|drawable)[^/]*\//);
	return marker === -1 ? '' : adaptivePath.slice(0, marker);
}

function moduleOf(resourceDir: string): string {
	const marker = resourceDir.search(/\/src\/[^/]+\/res$/);
	return marker === -1 ? resourceDir : resourceDir.slice(0, marker);
}

function siblingResourceDirs(tree: GithubTree, resourceDir: string): string[] {
	const module = moduleOf(resourceDir);
	if (module === resourceDir) return [resourceDir];

	const found = new Set<string>([resourceDir]);
	for (const path of blobs(tree)) {
		const match = path.match(/^(.*\/src\/[^/]+\/res)\//);
		const sibling = match?.[1];
		if (sibling !== undefined && moduleOf(sibling) === module) found.add(sibling);
	}

	return [...found];
}

export async function readColorResources(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	resourceDir: string
): Promise<Map<string, string>> {
	const buckets: { rank: number; path: string }[] = [];
	for (const directory of siblingResourceDirs(tree, resourceDir)) {
		const isOwn = directory === resourceDir;
		const loweredDirectory = directory.toLowerCase();

		for (const path of blobs(tree)) {
			const lowered = path.toLowerCase();
			if (!lowered.startsWith(`${loweredDirectory}/values`)) continue;
			if (!COLOR_VALUES_FILE.test(lowered)) continue;

			const qualifier = qualifierRank(lowered, loweredDirectory);
			if (qualifier === null) continue;
			buckets.push({ rank: qualifier + (isOwn ? SOURCE_SET_BONUS : 0), path });
		}
	}

	buckets.sort((left, right) => left.rank - right.rank || left.path.localeCompare(right.path));

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
): Promise<VectorIconResult | null> {
	const fidelity = createFidelity();

	const adaptivePath = declaredPath ?? findAdaptiveIconPath(tree);
	if (adaptivePath === null) return null;

	const adaptiveXml = await read(adaptivePath);
	if (adaptiveXml === null) return null;

	const refs = parseAdaptiveIcon(adaptiveXml);
	const rootKind = drawableKindOf(adaptiveXml);
	const isPlainVector = refs.foreground === null && rootKind === 'vector';
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

	const flatten = async (xml: string, depth: number): Promise<string | null> => {
		const kind = drawableKindOf(xml);
		if (kind === 'vector') return xml;

		if (kind !== 'layer-list') {
			markUnresolved(fidelity, `unsupported-drawable:${kind}`);
			return null;
		}

		if (depth >= MAX_LAYER_DEPTH) {
			markUnresolved(fidelity, 'layer-list-too-deep');
			return null;
		}

		const items = readLayerItems(xml);
		if (items.length === 0) {
			markUnresolved(fidelity, 'layer-list-empty');
			return null;
		}

		const rendered: string[] = [];
		for (const item of items) {
			if (item.kind === 'reference') {
				if (item.reference.kind !== 'drawable') {
					markUnresolved(fidelity, `layer-reference:${item.reference.kind}`);
					continue;
				}

				const nested = await readDrawable(item.reference.name);
				if (nested === null) {
					markUnresolved(fidelity, `missing-drawable:${item.reference.name}`);
					continue;
				}

				const flat = await flatten(nested, depth + 1);
				if (flat !== null) rendered.push(flat);
				continue;
			}

			const flat = await flatten(item.xml, depth + 1);
			if (flat !== null) rendered.push(flat);
		}

		if (rendered.length === 0) return null;

		return rendered.length === 1 ? (rendered[0] ?? null) : mergeVectors(rendered);
	};

	if (refs.foreground === null) {
		const svg = vectorToSvg(adaptiveXml, colors, { gradients, fidelity });
		return svg === null ? null : { svg: toDataUri(svg), fidelity };
	}

	if (refs.foreground.kind !== 'drawable') {
		markUnresolved(fidelity, `foreground-reference:${refs.foreground.kind}`);
		return null;
	}

	const declaredForeground = await readDrawable(refs.foreground.name);
	if (declaredForeground === null) {
		markUnresolved(fidelity, `missing-drawable:${refs.foreground.name}`);
		return null;
	}

	const foregroundXml = await flatten(declaredForeground, 0);
	if (foregroundXml === null) return null;

	let background: { kind: 'color' | 'vector'; value: string } | null = null;
	if (refs.background !== null) {
		if (refs.background.kind === 'color') {
			const reference = refs.background.name.startsWith('android:')
				? `@android:color/${refs.background.name.slice('android:'.length)}`
				: `@color/${refs.background.name}`;
			background = { kind: 'color', value: reference };
		} else if (refs.background.kind === 'mipmap') {
			markUnresolved(fidelity, 'background-reference:mipmap');
		} else {
			const backgroundXml = await readDrawable(refs.background.name);
			const shapeFill = backgroundXml === null ? null : readShapeFill(backgroundXml);

			if (shapeFill !== null) background = { kind: 'color', value: shapeFill };
			else if (backgroundXml !== null) {
				const flat = await flatten(backgroundXml, 0);
				if (flat !== null) background = { kind: 'vector', value: flat };
			} else markUnresolved(fidelity, `missing-drawable:${refs.background.name}`);
		}
	}

	const svg = composeAdaptiveSvg({
		background,
		foreground: foregroundXml,
		colors,
		gradients,
		fidelity
	});
	return svg === null ? null : { svg: toDataUri(svg), fidelity };
}
