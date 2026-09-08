import type { GithubTree } from '../github/types.ts';

export type PackageIdentity = {
	packageName: string | null;
	versionName: string | null;
	versionCode: string | null;
};

const GRADLE_FILENAMES = ['build.gradle', 'build.gradle.kts'];

const FLAVOUR_PENALTY = ['nightly', 'debug', 'dev', 'beta', 'alpha', 'staging', 'test', 'sample'];

const SECONDARY_MODULES = ['shell', 'server', 'daemon', 'cli', 'tool', 'benchmark'];

const PACKAGE_PATTERN = /^[a-z][a-z0-9_]*(\.[a-z0-9_]+)+$/i;

const QUOTED = String.raw`["']([^"']+)["']`;

const APPLICATION_ID = new RegExp(String.raw`\bapplicationId\s*(?:=|\s)\s*${QUOTED}`);
const APPLICATION_PLUGIN =
	/\bcom\.android\.application\b|\balias\(\s*libs\.plugins\.[a-z0-9.]*\b(?:android\.application|agp\.app(?:lication)?)\b[a-z0-9.]*\s*\)/i;
const NAMESPACE = new RegExp(String.raw`\bnamespace\s*(?:=|\s)\s*${QUOTED}`);
const MANIFEST_PACKAGE = new RegExp(String.raw`<manifest\b[^>]*?\bpackage\s*=\s*${QUOTED}`, 's');

const VERSION_NAME = new RegExp(String.raw`\bversionName\s*(?:=|\s)\s*${QUOTED}`);
const VERSION_CODE = /\bversionCode\s*(?:=|\s)\s*(\d+)/;

function isPlausiblePackage(value: string): boolean {
	const trimmed = value.trim();

	if (!PACKAGE_PATTERN.test(trimmed)) return false;
	if (trimmed.includes('$')) return false;

	return trimmed.split('.').length >= 2;
}

function firstMatch(source: string, pattern: RegExp): string | null {
	const match = pattern.exec(source);
	const value = match?.[1]?.trim();

	return value === undefined || value === '' ? null : value;
}

export function isApplicationModule(source: string): boolean {
	return APPLICATION_PLUGIN.test(stripComments(source));
}

export function parseGradlePackage(source: string): PackageIdentity {
	const stripped = stripComments(source);

	const applicationId = firstMatch(stripped, APPLICATION_ID);
	const namespace = APPLICATION_PLUGIN.test(stripped) ? firstMatch(stripped, NAMESPACE) : null;
	const candidate = applicationId ?? namespace;

	const versionName = firstMatch(stripped, VERSION_NAME);
	const versionCodeMatch = VERSION_CODE.exec(stripped);
	const versionCode = versionCodeMatch?.[1] ?? null;

	return {
		packageName: candidate !== null && isPlausiblePackage(candidate) ? candidate : null,
		versionName: versionName !== null && !versionName.includes('$') ? versionName : null,
		versionCode
	};
}

export function parseManifestPackage(source: string): string | null {
	const value = firstMatch(source, MANIFEST_PACKAGE);
	return value !== null && isPlausiblePackage(value) ? value : null;
}

function stripComments(source: string): string {
	return source.replace(/\/\*[\s\S]*?\*\//g, ' ').replace(/(^|\s)\/\/[^\n]*/g, '$1');
}

function flavourRank(path: string): number {
	const lowered = path.toLowerCase();

	if (lowered.includes('/src/main/')) return 0;
	return FLAVOUR_PENALTY.some((flavour) => lowered.includes(`/src/${flavour}/`)) ? 2 : 1;
}

function depthRank(path: string): number {
	return path.split('/').length;
}

function moduleRank(path: string): number {
	const module = path.toLowerCase().split('/').slice(0, -1).pop() ?? '';

	if (module === 'app' || module === 'manager') return 0;
	if (SECONDARY_MODULES.includes(module)) return 3;
	if (FLAVOUR_PENALTY.includes(module)) return 4;

	return module === '' ? 2 : 1;
}

function rankOf(path: string): number[] {
	return [flavourRank(path), moduleRank(path), depthRank(path)];
}

function compareRank(left: string, right: string): number {
	const a = rankOf(left);
	const b = rankOf(right);

	for (let index = 0; index < a.length; index += 1) {
		const difference = (a[index] ?? 0) - (b[index] ?? 0);
		if (difference !== 0) return difference;
	}

	return left.localeCompare(right);
}

export function findGradleFiles(tree: GithubTree): string[] {
	return tree.tree
		.filter((entry) => entry.type === 'blob')
		.map((entry) => entry.path)
		.filter((path) => GRADLE_FILENAMES.includes(path.split('/').pop() ?? ''))
		.sort(compareRank);
}

export function findManifestFiles(tree: GithubTree): string[] {
	return tree.tree
		.filter((entry) => entry.type === 'blob')
		.map((entry) => entry.path)
		.filter((path) => path.endsWith('AndroidManifest.xml'))
		.sort(compareRank);
}

const MAX_FILES_READ = 12;

export async function resolvePackageIdentity(
	tree: GithubTree | null,
	read: (path: string) => Promise<string | null>
): Promise<PackageIdentity> {
	const empty: PackageIdentity = { packageName: null, versionName: null, versionCode: null };
	if (tree === null) return empty;

	for (const path of findGradleFiles(tree).slice(0, MAX_FILES_READ)) {
		const source = await read(path);
		if (source === null) continue;
		if (!isApplicationModule(source)) continue;

		const parsed = parseGradlePackage(source);
		if (parsed.packageName !== null) return parsed;
	}

	for (const path of findManifestFiles(tree).slice(0, MAX_FILES_READ)) {
		const source = await read(path);
		if (source === null) continue;

		const packageName = parseManifestPackage(source);
		if (packageName !== null) {
			return { packageName, versionName: null, versionCode: null };
		}
	}

	return empty;
}
