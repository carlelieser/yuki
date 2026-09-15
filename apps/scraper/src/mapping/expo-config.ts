import type { GithubTree } from '@yuki/github';
import { blobs, resolveRelativePath } from './icon.ts';

const CONFIG_FILES = ['app.json', 'app.config.json'];

const RASTER_SUFFIX = /\.(png|webp|jpg|jpeg)$/i;

export type ExpoIcon = { foreground: string; background: string | null };

type ExpoAdaptiveIcon = { foregroundImage?: unknown; backgroundColor?: unknown };

type ExpoSection = { icon?: unknown; android?: { adaptiveIcon?: ExpoAdaptiveIcon } };

export function findExpoConfigPaths(tree: GithubTree): string[] {
	return blobs(tree)
		.filter((path) => CONFIG_FILES.includes(path.split('/').pop() ?? ''))
		.sort((left, right) => left.split('/').length - right.split('/').length);
}

function asString(value: unknown): string | null {
	return typeof value === 'string' && value !== '' ? value : null;
}

function sectionOf(parsed: unknown): ExpoSection | null {
	if (typeof parsed !== 'object' || parsed === null) return null;

	const root = parsed as { expo?: unknown };
	const expo = typeof root.expo === 'object' && root.expo !== null ? root.expo : parsed;

	return expo as ExpoSection;
}

export function readExpoIcon(json: string): ExpoIcon | null {
	let parsed: unknown;
	try {
		parsed = JSON.parse(json);
	} catch {
		return null;
	}

	const expo = sectionOf(parsed);
	if (expo === null) return null;

	const adaptive = expo.android?.adaptiveIcon;
	const foreground = asString(adaptive?.foregroundImage) ?? asString(expo.icon);
	if (foreground === null || !RASTER_SUFFIX.test(foreground)) return null;

	return { foreground, background: asString(adaptive?.backgroundColor) };
}

export function resolveExpoAsset(
	tree: GithubTree,
	configPath: string,
	asset: string
): string | null {
	const resolved = resolveRelativePath(configPath, asset);
	if (resolved === null) return null;

	return blobs(tree).includes(resolved) ? resolved : null;
}
