import type { GithubTree } from '@yuki/github';
import {
	composeAdaptiveSvg,
	parseAdaptiveIcon,
	parseColors,
	toDataUri,
	vectorToSvg
} from './vector-icon.ts';

const RASTER_EXTENSIONS = ['.png', '.webp', '.jpg', '.jpeg'];

const ICON_STEMS = ['ic_launcher', 'ic_launcher_round', 'ic_launcher_foreground'];
const STEM_ORDER = ICON_STEMS;
const LAUNCHER_STEM = /^(?:[a-z0-9]+_)?(?:ic_)?launcher(?:_round|_foreground)?$/;

const DENSITY_ORDER = ['xxxhdpi', 'xxhdpi', 'xhdpi', 'hdpi', 'mdpi'];

const FASTLANE_ICON =
	/(^|\/)(fastlane\/)?metadata\/(android\/)?([^/]+\/)?images\/icon\.(png|webp|jpg|jpeg)$/;

const LOCALE_ORDER = ['en-us', 'en-gb', 'en'];

const LFS_POINTER_MAX_BYTES = 200;
const LFS_POINTER_PREFIX = 'version https://git-lfs.github.com/spec/v1';
const MEDIA_EXTENSIONS = ['.png', '.webp', '.jpg', '.jpeg', '.gif', '.svg'];
const PLAYSTORE_ICON = /(^|\/)ic_launcher[-_]playstore\.(png|webp|jpg|jpeg)$/;

const FLAVOUR_PENALTY = ['nightly', 'debug', 'dev', 'beta', 'alpha', 'staging', 'test'];

const MANIFEST_FILE = /(^|\/)AndroidManifest\.xml$/;
const APPLICATION_TAG = /<application\b[^>]*>/;
const MANIFEST_ICON = /android:(?:roundIcon|icon)="@(drawable|mipmap)\/([A-Za-z0-9_]+)"/g;

export function findManifestPath(tree: GithubTree): string | null {
	return pickBest(blobs(tree).filter((path) => MANIFEST_FILE.test(path)));
}

export function readManifestIcon(xml: string): { kind: string; name: string } | null {
	const application = xml.match(APPLICATION_TAG)?.[0];
	if (application === undefined) return null;

	const icons = [...application.matchAll(MANIFEST_ICON)];
	const round = icons.find((match) => match[0].includes('roundIcon'));
	const chosen = icons.find((match) => !match[0].includes('roundIcon')) ?? round;
	if (chosen === undefined) return null;

	const kind = chosen[1];
	const name = chosen[2];
	if (kind === undefined || name === undefined) return null;

	return { kind, name };
}


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

function localeRank(path: string): number {
	const match = path.match(FASTLANE_ICON);
	if (match === null) return 0;

	const locale = match[4]?.replace(/\/$/, '') ?? '';
	if (locale === '') return 0;

	const index = LOCALE_ORDER.indexOf(locale);
	return index === -1 ? LOCALE_ORDER.length + 1 : index + 1;
}

function rankOf(path: string): number[] {
	const lowered = path.toLowerCase();
	const { stem } = splitPath(lowered);
	return [flavourRank(lowered), localeRank(lowered), densityRank(lowered), stemRank(stem)];
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

			return LAUNCHER_STEM.test(stem);
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

	const entry = tree.tree.find((candidate) => candidate.path === resolved);
	let isLfs = false;

	if (
		entry?.sha !== undefined &&
		entry.size !== undefined &&
		entry.size <= LFS_POINTER_MAX_BYTES
	) {
		const content = await readBlob(entry.sha);
		isLfs = content !== null && isLfsPointer(content);
	}

	return buildBlobUrl(owner, name, defaultBranch, resolved, isLfs);
}

export function findDeclaredIconPaths(
	tree: GithubTree,
	icon: { kind: string; name: string }
): { xml: string[]; raster: string[] } {
	const suffix = `/${icon.kind}`;
	const xml: string[] = [];
	const raster: string[] = [];

	for (const path of blobs(tree)) {
		const { dir, filename, stem } = splitPath(path);
		if (stem !== icon.name) continue;

		const directory = dir.split('/').pop() ?? '';
		if (!directory.startsWith(`${icon.kind}`) && !directory.startsWith(suffix.slice(1))) continue;

		if (filename.endsWith('.xml')) xml.push(path);
		else if (isRaster(filename)) raster.push(path);
	}

	return { xml, raster };
}

export function pickBestDeclared(candidates: string[]): string | null {
	return pickBest(candidates);
}

const RESOURCE_REFERENCE = /android:(?:src|drawable)="@(drawable|mipmap)\/([A-Za-z0-9_]+)"/g;
const ADAPTIVE_RASTER_LAYER =
	/<(background|foreground)\b[^>]*android:drawable="@(drawable|mipmap)\/([A-Za-z0-9_]+)"/g;

export type AdaptiveRasterLayers = {
	background: { kind: string; name: string } | null;
	foreground: { kind: string; name: string } | null;
};

export function readAdaptiveRasterLayers(xml: string): AdaptiveRasterLayers {
	const layers: AdaptiveRasterLayers = { background: null, foreground: null };

	for (const match of xml.matchAll(ADAPTIVE_RASTER_LAYER)) {
		const layer = match[1];
		const kind = match[2];
		const name = match[3];
		if (kind === undefined || name === undefined) continue;

		if (layer === 'background') layers.background = { kind, name };
		if (layer === 'foreground') layers.foreground = { kind, name };
	}

	return layers;
}

export function readRasterReferences(xml: string): { kind: string; name: string }[] {
	const found: { kind: string; name: string }[] = [];

	for (const match of xml.matchAll(RESOURCE_REFERENCE)) {
		const kind = match[1];
		const name = match[2];
		if (kind === undefined || name === undefined) continue;
		found.push({ kind, name });
	}

	return found;
}

export function findRasterForReference(
	tree: GithubTree,
	reference: { kind: string; name: string }
): string | null {
	return pickBest(
		blobs(tree).filter((path) => {
			const { dir, filename, stem } = splitPath(path);
			if (stem !== reference.name || !isRaster(filename)) return false;

			const directory = dir.split('/').pop() ?? '';
			return new RegExp(`^${reference.kind}(-|$)`).test(directory);
		})
	);
}

export function findAdaptiveIconPath(tree: GithubTree): string | null {
	return pickBest(
		blobs(tree).filter((path) => {
			const lowered = path.toLowerCase();
			if (!lowered.includes('mipmap-anydpi')) return false;

			const { stem, filename } = splitPath(lowered);
			if (!filename.endsWith('.xml')) return false;

			return LAUNCHER_STEM.test(stem);
		})
	);
}

function qualifierRank(loweredPath: string, loweredResourceDir: string): number | null {
	const rest = loweredPath.slice(`${loweredResourceDir}/`.length);
	const dir = rest.split('/')[0] ?? '';
	const qualifiers = dir.split('-').slice(1);

	if (qualifiers.some((qualifier) => /^v\d+$/.test(qualifier))) return null;

	if (qualifiers.length === 0) return 2;
	if (qualifiers.length === 1 && qualifiers[0] === 'night') return 1;

	return null;
}

function resourceDirOf(adaptivePath: string): string {
	const marker = adaptivePath.search(/\/(mipmap|drawable)[^/]*\//);
	return marker === -1 ? '' : adaptivePath.slice(0, marker);
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

	const buckets: { rank: number; path: string }[] = [];
	for (const path of blobs(tree)) {
		const lowered = path.toLowerCase();
		if (!lowered.startsWith(`${resourceDir.toLowerCase()}/values`)) continue;
		if (!/\/(colors?|ic_launcher_background)\.xml$/.test(lowered)) continue;

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
