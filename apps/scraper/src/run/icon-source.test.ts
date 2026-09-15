import { describe, expect, it } from 'vitest';
import type { GithubClient, GithubTree } from '@yuki/github';
import { iconFrom } from './icon-source.ts';

const ADAPTIVE = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
	<background android:drawable="@color/ic_launcher_background" />
	<foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>`;

const INSET_BITMAP = `<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
	android:insetLeft="18dp" android:insetTop="18dp">
	<bitmap android:gravity="fill" android:src="@drawable/aether_mark" />
</inset>`;

const PLACEHOLDER_MANIFEST = `<manifest xmlns:android="http://schemas.android.com/apk/res/android">
	<application android:icon="\${appIcon}" android:label="\${appLabel}" />
</manifest>`;

function tree(paths: string[]): GithubTree {
	return { tree: paths.map((path) => ({ path, type: 'blob' })), truncated: false };
}

function clientReading(files: Map<string, string>): GithubClient {
	return {
		getRawFile: (_owner: string, _name: string, path: string) =>
			Promise.resolve(
				files.has(path)
					? { isModified: true, body: files.get(path), etag: null }
					: { isModified: false }
			),
		getBlob: () => Promise.resolve({ isModified: false })
	} as unknown as GithubClient;
}

describe('adaptive icons backed by a raster drawable', () => {
	const paths = [
		'app/src/main/AndroidManifest.xml',
		'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
		'app/src/main/res/drawable/ic_launcher_foreground.xml',
		'app/src/main/res/drawable-nodpi/aether_mark.png',
		'app/src/main/res/values/colors.xml'
	];

	const files = new Map([
		['app/src/main/AndroidManifest.xml', PLACEHOLDER_MANIFEST],
		['app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml', ADAPTIVE],
		['app/src/main/res/drawable/ic_launcher_foreground.xml', INSET_BITMAP],
		[
			'app/src/main/res/values/colors.xml',
			`<resources><color name="ic_launcher_background">#FFFFFF</color></resources>`
		]
	]);

	it('falls back to the raster a foreground drawable wraps', async () => {
		const icon = await iconFrom(clientReading(files), tree(paths), 'owner', 'repo', 'main');

		expect(icon).toBe(
			'https://raw.githubusercontent.com/owner/repo/main/app/src/main/res/drawable-nodpi/aether_mark.png'
		);
	});

	it('returns null when the wrapped raster is absent from the tree', async () => {
		const icon = await iconFrom(
			clientReading(files),
			tree(paths.filter((path) => !path.endsWith('aether_mark.png'))),
			'owner',
			'repo',
			'main'
		);

		expect(icon).toBeNull();
	});
});

describe('expo projects', () => {
	const EXPO_CONFIG = JSON.stringify({
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

	it('resolves an icon declared in app.json when there is no android resource tree', async () => {
		const icon = await iconFrom(
			clientReading(new Map([['app.json', EXPO_CONFIG]])),
			tree(['app.json', 'package.json', 'assets/icon.png', 'assets/adaptive-icon.png']),
			'owner',
			'repo',
			'main'
		);

		expect(icon).toBe('https://raw.githubusercontent.com/owner/repo/main/assets/adaptive-icon.png');
	});

	it('leaves android projects to the resource pipeline', async () => {
		const icon = await iconFrom(
			clientReading(new Map([['app.json', EXPO_CONFIG]])),
			tree([
				'app.json',
				'assets/adaptive-icon.png',
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher.png'
			]),
			'owner',
			'repo',
			'main'
		);

		expect(icon).toBe(
			'https://raw.githubusercontent.com/owner/repo/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png'
		);
	});

	it('returns null when the declared asset is missing from the tree', async () => {
		const icon = await iconFrom(
			clientReading(new Map([['app.json', EXPO_CONFIG]])),
			tree(['app.json', 'package.json']),
			'owner',
			'repo',
			'main'
		);

		expect(icon).toBeNull();
	});
});
