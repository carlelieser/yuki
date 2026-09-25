import type { GithubTree } from '@yuki/github';
import { blobs, findAdaptiveIconPath, findRasterForReference, splitPath } from './icon.ts';
import { parseAdaptiveIcon, parseColors, vectorToSvg } from './vector-icon.ts';
import { readShapeFill } from './solid-layer.ts';
import { composeAdaptiveSvg, toDataUri } from './adaptive-svg.ts';
import { drawableKindOf } from './drawable-kind.ts';
import { createFidelity, markUnresolved, type Fidelity } from './fidelity.ts';
import {
	flattenDrawable,
	flattenReference,
	rasterLayer,
	type DrawableReference,
	type DrawableSources
} from './drawable-layers.ts';

export type VectorIconResult = { svg: string; fidelity: Fidelity };

export type VectorIconOptions = {
	declaredPath?: string | null;
	download?: (path: string) => Promise<Buffer | null>;
};

const RASTER_TYPES: Record<string, string> = {
	png: 'image/png',
	webp: 'image/webp',
	jpg: 'image/jpeg',
	jpeg: 'image/jpeg'
};

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

type Background = { kind: 'color' | 'vector'; value: string } | null;

export async function buildVectorIcon(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	options: VectorIconOptions = {}
): Promise<VectorIconResult | null> {
	const fidelity = createFidelity();

	const adaptivePath = options.declaredPath ?? findAdaptiveIconPath(tree);
	if (adaptivePath === null) return null;

	const adaptiveXml = await read(adaptivePath);
	if (adaptiveXml === null) return null;

	const refs = parseAdaptiveIcon(adaptiveXml);
	const isPlainVector = refs.foreground === null && drawableKindOf(adaptiveXml) === 'vector';
	if (refs.foreground === null && !isPlainVector) return null;

	const resourceDir = resourceDirOf(adaptivePath);
	const colors = await readColorResources(tree, read, resourceDir);
	const gradients = await readColorGradients(tree, read, resourceDir);

	if (refs.foreground === null) {
		const svg = vectorToSvg(adaptiveXml, colors, { gradients, fidelity });
		return svg === null ? null : { svg: toDataUri(svg), fidelity };
	}

	const sources: DrawableSources = {
		readDrawable: drawableReaderFor(tree, read, resourceDir),
		readRaster: options.download === undefined ? null : rasterReaderFor(tree, options.download),
		fidelity
	};

	const foreground = await foregroundLayer(refs.foreground, sources);
	if (foreground === null) return null;

	const background = await backgroundLayer(refs.background, sources);
	const svg = composeAdaptiveSvg({ background, foreground, colors, gradients, fidelity });
	return svg === null ? null : { svg: toDataUri(svg), fidelity };
}

async function foregroundLayer(
	reference: DrawableReference,
	sources: DrawableSources
): Promise<string | null> {
	if (reference.kind === 'color') {
		markUnresolved(sources.fidelity, `foreground-reference:${reference.kind}`);
		return null;
	}
	if (reference.kind === 'mipmap') {
		return rasterLayer(reference, sources, 'foreground-reference:mipmap');
	}

	return flattenReference(reference, sources);
}

async function backgroundLayer(
	reference: DrawableReference | null,
	sources: DrawableSources
): Promise<Background> {
	if (reference === null) return null;

	if (reference.kind === 'color') {
		const value = reference.name.startsWith('android:')
			? `@android:color/${reference.name.slice('android:'.length)}`
			: `@color/${reference.name}`;
		return { kind: 'color', value };
	}

	if (reference.kind === 'mipmap') {
		const raster = await rasterLayer(reference, sources, 'background-reference:mipmap');
		return raster === null ? null : { kind: 'vector', value: raster };
	}

	const xml = await sources.readDrawable(reference.name);
	const shapeFill = xml === null ? null : readShapeFill(xml);
	if (shapeFill !== null) return { kind: 'color', value: shapeFill };

	const flat =
		xml === null ? await flattenReference(reference, sources) : await flattenDrawable(xml, sources);
	return flat === null ? null : { kind: 'vector', value: flat };
}

async function readColorGradients(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	resourceDir: string
): Promise<Map<string, string>> {
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

	return gradients;
}

function drawableReaderFor(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	resourceDir: string
): (drawable: string) => Promise<string | null> {
	const prefix = `${resourceDir}/drawable`;

	return async (drawable) => {
		const candidates = blobs(tree).filter((path) => {
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
}

function rasterReaderFor(
	tree: GithubTree,
	download: (path: string) => Promise<Buffer | null>
): (reference: DrawableReference) => Promise<string | null> {
	return async (reference) => {
		const path = findRasterForReference(tree, reference);
		if (path === null) return null;

		const type = RASTER_TYPES[path.slice(path.lastIndexOf('.') + 1).toLowerCase()];
		if (type === undefined) return null;

		const bytes = await download(path);
		return bytes === null ? null : `data:${type};base64,${bytes.toString('base64')}`;
	};
}
