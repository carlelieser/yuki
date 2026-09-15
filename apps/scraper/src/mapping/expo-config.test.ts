import { describe, expect, it } from 'vitest';
import type { GithubTree } from '@yuki/github';
import { findExpoConfigPaths, readExpoIcon, resolveExpoAsset } from './expo-config.ts';

function tree(paths: string[]): GithubTree {
	return { tree: paths.map((path) => ({ path, type: 'blob' })), truncated: false };
}

const SYNCCLIPBOARD = JSON.stringify({
	expo: {
		name: 'SyncClipboard',
		icon: './assets/icon.png',
		android: {
			adaptiveIcon: {
				foregroundImage: './assets/adaptive-icon.png',
				backgroundColor: '#ffffff'
			}
		}
	}
});

describe('readExpoIcon', () => {
	it('prefers the android adaptive foreground over the shared icon', () => {
		expect(readExpoIcon(SYNCCLIPBOARD)).toEqual({
			foreground: './assets/adaptive-icon.png',
			background: '#ffffff'
		});
	});

	it('falls back to the shared icon when no adaptive icon is declared', () => {
		const json = JSON.stringify({ expo: { icon: './assets/icon.png' } });

		expect(readExpoIcon(json)).toEqual({ foreground: './assets/icon.png', background: null });
	});

	it('reads a config that omits the expo wrapper', () => {
		const json = JSON.stringify({ icon: './assets/icon.png' });

		expect(readExpoIcon(json)?.foreground).toBe('./assets/icon.png');
	});

	it('ignores an icon that is not a raster asset', () => {
		expect(readExpoIcon(JSON.stringify({ expo: { icon: './assets/icon.svg' } }))).toBeNull();
	});

	it('returns null when the config declares no icon', () => {
		expect(readExpoIcon(JSON.stringify({ expo: { name: 'App' } }))).toBeNull();
	});

	it('returns null for a config that is not valid json', () => {
		expect(readExpoIcon('module.exports = { expo: {} }')).toBeNull();
	});
});

describe('findExpoConfigPaths', () => {
	it('prefers a root config over one nested in a subdirectory', () => {
		expect(findExpoConfigPaths(tree(['examples/demo/app.json', 'app.json']))[0]).toBe('app.json');
	});

	it('ignores unrelated json files', () => {
		expect(findExpoConfigPaths(tree(['package.json', 'tsconfig.json']))).toEqual([]);
	});
});

describe('resolveExpoAsset', () => {
	it('resolves an asset declared relative to the config', () => {
		expect(
			resolveExpoAsset(tree(['app.json', 'assets/icon.png']), 'app.json', './assets/icon.png')
		).toBe('assets/icon.png');
	});

	it('resolves an asset alongside a nested config', () => {
		expect(
			resolveExpoAsset(
				tree(['mobile/app.json', 'mobile/assets/icon.png']),
				'mobile/app.json',
				'./assets/icon.png'
			)
		).toBe('mobile/assets/icon.png');
	});

	it('returns null when the declared asset is absent from the tree', () => {
		expect(resolveExpoAsset(tree(['app.json']), 'app.json', './assets/icon.png')).toBeNull();
	});
});
