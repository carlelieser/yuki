import type { GithubTree } from '@yuki/github';
import type { ResourceReference } from './android-resources.ts';
import { buildBlobUrl, isLfsBlob } from './blob-url.ts';
export { buildBlobUrl, collectLfsPaths, isLfsPointer } from './blob-url.ts';
export {
	readAdaptiveRasterLayers,
	readManifestIcon,
	readRasterReferences,
	type AdaptiveRasterLayers,
	type ResourceReference
} from './android-resources.ts';

const RASTER_EXTENSIONS = ['.png', '.webp', '.jpg', '.jpeg'];

const ICON_STEMS = ['ic_launcher', 'ic_launcher_round', 'ic_launcher_foreground'];
const STEM_ORDER = ICON_STEMS;
const LAUNCHER_SUFFIX = '(?:_(?:round|foreground|adaptive))*';
const LAUNCHER_STEM = new RegExp(`^(?:[a-z0-9]+_)*(?:ic_)?launcher${LAUNCHER_SUFFIX}$`);

const LAUNCHER_EXCLUDED = /(?:^|_)(shortcut|notification|monochrome|badge|widget|tile)(?:_|$)/;

const RESOURCE_DIR = /(^|\/)res\/(mipmap|drawable)(-[a-z0-9-]+)?$/;

const DENSITY_ORDER = ['xxxhdpi', 'xxhdpi', 'xhdpi', 'hdpi', 'mdpi'];

const FASTLANE_ICON =
	/(^|\/)(fastlane\/)?metadata\/(android\/)?([^/]+\/)?images\/icon\.(png|webp|jpg|jpeg)$/;

const LOCALE_ORDER = ['en-us', 'en-gb', 'en'];

const PLAYSTORE_ICON = /(^|\/)ic_launcher[-_]playstore\.(png|webp|jpg|jpeg)$/;

const FLAVOUR_PENALTY = ['nightly', 'debug', 'dev', 'beta', 'alpha', 'staging', 'test'];

const MANIFEST_FILE = /(^|\/)AndroidManifest\.xml$/;

export function findManifestPath(tree: GithubTree): string | null {
	return pickBest(findManifestPaths(tree));
}

export function findManifestPaths(tree: GithubTree): string[] {
	return blobs(tree)
		.filter((path) => MANIFEST_FILE.test(path))
		.sort((left, right) => (isBetter(left, right) ? -1 : isBetter(right, left) ? 1 : 0));
}

export function splitPath(path: string): { dir: string; filename: string; stem: string } {
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

function layerRank(stem: string): number {
	return stem.endsWith('_foreground') ? 1 : 0;
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
	return [
		flavourRank(lowered),
		localeRank(lowered),
		layerRank(stem),
		densityRank(lowered),
		stemRank(stem)
	];
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

export function blobs(tree: GithubTree): string[] {
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

function isLauncherStem(stem: string): boolean {
	return LAUNCHER_STEM.test(stem) && !LAUNCHER_EXCLUDED.test(stem);
}

function isResourceDir(dir: string): boolean {
	return RESOURCE_DIR.test(dir);
}

function findMipmapIcon(paths: string[]): string | null {
	return pickBest(
		paths.filter((path) => {
			const { dir, filename, stem } = splitPath(path.toLowerCase());
			if (!isResourceDir(dir) || !isRaster(filename)) return false;

			return isLauncherStem(stem);
		})
	);
}

function findByPattern(paths: string[], pattern: RegExp): string | null {
	return pickBest(paths.filter((path) => pattern.test(path.toLowerCase())));
}

export function findCuratedIconPath(tree: GithubTree): string | null {
	return findByPattern(blobs(tree), FASTLANE_ICON);
}

export function findPrebakedIconPath(tree: GithubTree): string | null {
	const paths = blobs(tree);

	return findByPattern(paths, PLAYSTORE_ICON) ?? findMipmapIcon(paths);
}

export function findIconPath(tree: GithubTree): string | null {
	return findCuratedIconPath(tree) ?? findPrebakedIconPath(tree);
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
	readBlob: (sha: string) => Promise<string | null>,
	find: (tree: GithubTree) => string | null = findIconPath
): Promise<string | null> {
	const path = find(tree);
	if (path === null) return null;

	const resolved = await resolveSymlinkPath(tree, path, readBlob);
	if (resolved === null) return null;

	const entry = tree.tree.find((candidate) => candidate.path === resolved);
	const isLfs = await isLfsBlob(entry, readBlob);

	return buildBlobUrl(owner, name, defaultBranch, resolved, isLfs);
}

export function findDeclaredIconPaths(
	tree: GithubTree,
	icon: ResourceReference
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

export function findRasterForReference(
	tree: GithubTree,
	reference: ResourceReference
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

			return isLauncherStem(stem);
		})
	);
}
