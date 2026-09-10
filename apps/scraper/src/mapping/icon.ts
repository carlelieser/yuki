import type { GithubTree } from '@yuki/github';
import { composeAdaptiveSvg, parseAdaptiveIcon, parseColors, toDataUri } from './vector-icon.ts';

const RASTER_EXTENSIONS = ['.png', '.webp', '.jpg', '.jpeg'];

const ICON_STEMS = ['ic_launcher', 'ic_launcher_round', 'ic_launcher_foreground'];
const STEM_ORDER = ICON_STEMS;

const DENSITY_ORDER = ['xxxhdpi', 'xxhdpi', 'xhdpi', 'hdpi', 'mdpi'];

const FASTLANE_ICON =
	/(^|\/)fastlane\/metadata\/android\/[^/]+\/images\/icon\.(png|webp|jpg|jpeg)$/;
const PLAYSTORE_ICON = /(^|\/)ic_launcher[-_]playstore\.(png|webp|jpg|jpeg)$/;

const FLAVOUR_PENALTY = ['nightly', 'debug', 'dev', 'beta', 'alpha', 'staging', 'test'];

const ADAPTIVE_STEMS = ['ic_launcher', 'launcher', 'ic_launcher_foreground'];

function splitPath(path: string): { dir: string; filename: string; stem: string } {
	const segments = path.split('/');
	const filename = segments.pop() ?? '';
	const stem = filename.replace(/\.[a-z0-9]+$/, '');
	return { dir: segments.join('/'), filename, stem };
}

function isRaster(filename: string): boolean {
	return RASTER_EXTENSIONS.some((extension) => filename.endsWith(extension));
}

function flavourRank(path: string): number {
	if (path.includes('/src/main/')) return 0;
	return FLAVOUR_PENALTY.some((flavour) => path.includes(`/src/${flavour}/`)) ? 2 : 1;
}

function densityRank(path: string): number {
	const index = DENSITY_ORDER.findIndex((density) => path.includes(`mipmap-${density}`));
	return index === -1 ? DENSITY_ORDER.length : index;
}

function stemRank(stem: string): number {
	const index = STEM_ORDER.indexOf(stem);
	return index === -1 ? STEM_ORDER.length : index;
}

function rankOf(path: string): number[] {
	const { stem } = splitPath(path.toLowerCase());
	return [flavourRank(path.toLowerCase()), densityRank(path.toLowerCase()), stemRank(stem)];
}

function isBetter(candidate: string, best: string): boolean {
	const left = rankOf(candidate);
	const right = rankOf(best);

	for (let index = 0; index < left.length; index += 1) {
		const a = left[index] ?? 0;
		const b = right[index] ?? 0;
		if (a !== b) return a < b;
	}

	return false;
}

const SYMLINK_MODE = '120000';
const MAX_SYMLINK_HOPS = 5;

function blobs(tree: GithubTree): string[] {
	return tree.tree.filter((entry) => entry.type === 'blob').map((entry) => entry.path);
}

function symlinkShas(tree: GithubTree): Map<string, string> {
	const entries = new Map<string, string>();

	for (const entry of tree.tree) {
		if (entry.type !== 'blob' || entry.mode !== SYMLINK_MODE) continue;
		if (entry.sha === undefined) continue;
		entries.set(entry.path, entry.sha);
	}

	return entries;
}

export function resolveRelativePath(fromPath: string, target: string): string | null {
	const base = fromPath.split('/').slice(0, -1);
	const segments = target.split('/');

	for (const segment of segments) {
		if (segment === '' || segment === '.') continue;

		if (segment === '..') {
			if (base.length === 0) return null;
			base.pop();
			continue;
		}

		base.push(segment);
	}

	return base.length === 0 ? null : base.join('/');
}

function pickBest(candidates: string[]): string | null {
	if (candidates.length === 0) return null;
	return candidates.reduce((best, path) => (isBetter(path, best) ? path : best));
}

function findMipmapIcon(paths: string[]): string | null {
	return pickBest(
		paths.filter((path) => {
			const lowered = path.toLowerCase();
			if (!lowered.includes('mipmap-') && !lowered.includes('drawable-')) return false;

			const { filename, stem } = splitPath(lowered);
			if (!isRaster(filename)) return false;

			return ICON_STEMS.includes(stem);
		})
	);
}

function findByPattern(paths: string[], pattern: RegExp): string | null {
	return pickBest(paths.filter((path) => pattern.test(path.toLowerCase())));
}

export function findIconPath(tree: GithubTree): string | null {
	const paths = blobs(tree);

	return (
		findByPattern(paths, FASTLANE_ICON) ??
		findByPattern(paths, PLAYSTORE_ICON) ??
		findMipmapIcon(paths)
	);
}

export async function resolveSymlinkPath(
	tree: GithubTree,
	path: string,
	readBlob: (sha: string) => Promise<string | null>
): Promise<string | null> {
	const links = symlinkShas(tree);
	const available = new Set(blobs(tree));

	let current = path;
	for (let hop = 0; hop < MAX_SYMLINK_HOPS; hop += 1) {
		const sha = links.get(current);
		if (sha === undefined) return available.has(current) ? current : null;

		const target = await readBlob(sha);
		if (target === null) return null;

		const resolved = resolveRelativePath(current, target.trim());
		if (resolved === null || resolved === current) return null;

		current = resolved;
	}

	return null;
}

export async function buildIconUrl(
	tree: GithubTree,
	owner: string,
	name: string,
	defaultBranch: string,
	readBlob: (sha: string) => Promise<string | null>
): Promise<string | null> {
	const path = findIconPath(tree);
	if (path === null) return null;

	const resolved = await resolveSymlinkPath(tree, path, readBlob);
	if (resolved === null) return null;

	const encoded = resolved
		.split('/')
		.map((segment) => encodeURIComponent(segment))
		.join('/');

	return `https://raw.githubusercontent.com/${owner}/${name}/${defaultBranch}/${encoded}`;
}

export function findAdaptiveIconPath(tree: GithubTree): string | null {
	return pickBest(
		blobs(tree).filter((path) => {
			const lowered = path.toLowerCase();
			if (!lowered.includes('mipmap-anydpi')) return false;

			const { stem, filename } = splitPath(lowered);
			if (!filename.endsWith('.xml')) return false;

			return ADAPTIVE_STEMS.some(
				(candidate) => stem === candidate || stem === `${candidate}_round`
			);
		})
	);
}

function resourceDirOf(adaptivePath: string): string {
	const marker = adaptivePath.lastIndexOf('/mipmap-anydpi');
	return marker === -1 ? '' : adaptivePath.slice(0, marker);
}

export async function buildVectorIcon(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>
): Promise<string | null> {
	const adaptivePath = findAdaptiveIconPath(tree);
	if (adaptivePath === null) return null;

	const adaptiveXml = await read(adaptivePath);
	if (adaptiveXml === null) return null;

	const refs = parseAdaptiveIcon(adaptiveXml);
	if (refs.foreground === null) return null;

	const resourceDir = resourceDirOf(adaptivePath);
	const available = new Set(blobs(tree));

	const colors = new Map<string, string>();
	for (const path of blobs(tree)) {
		const lowered = path.toLowerCase();
		if (!lowered.startsWith(`${resourceDir.toLowerCase()}/values`)) continue;
		if (!/\/(colors?|ic_launcher_background)\.xml$/.test(lowered)) continue;

		const xml = await read(path);
		if (xml === null) continue;
		for (const [key, value] of parseColors(xml)) colors.set(key, value);
	}

	const readDrawable = async (drawable: string): Promise<string | null> => {
		const candidates = [
			`${resourceDir}/drawable/${drawable}.xml`,
			`${resourceDir}/drawable-v24/${drawable}.xml`,
			`${resourceDir}/drawable-anydpi/${drawable}.xml`
		];

		for (const candidate of candidates) {
			if (available.has(candidate)) return read(candidate);
		}

		return null;
	};

	const foregroundXml =
		refs.foreground.kind === 'drawable' ? await readDrawable(refs.foreground.name) : null;
	if (foregroundXml === null) return null;

	let background: { kind: 'color' | 'vector'; value: string } | null = null;
	if (refs.background !== null) {
		if (refs.background.kind === 'color') {
			background = { kind: 'color', value: `@color/${refs.background.name}` };
		} else {
			const backgroundXml = await readDrawable(refs.background.name);
			if (backgroundXml !== null) background = { kind: 'vector', value: backgroundXml };
		}
	}

	const svg = composeAdaptiveSvg({ background, foreground: foregroundXml, colors });
	return svg === null ? null : toDataUri(svg);
}
