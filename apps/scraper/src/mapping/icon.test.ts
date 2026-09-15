import { describe, expect, it } from 'vitest';
import {
	buildIconUrl,
	buildVectorIcon,
	findAdaptiveIconPath,
	findDeclaredIconPaths,
	findIconPath,
	findManifestPath,
	findRasterForReference,
	readManifestIcon,
	readRasterReferences,
	resolveRelativePath
} from './icon.ts';
import type { GithubTree } from '@yuki/github';

function tree(paths: string[], truncated = false): GithubTree {
	return {
		tree: paths.map((path) => ({ path, type: 'blob' })),
		truncated
	};
}

function treeWithSymlink(paths: string[], symlinkPath: string): GithubTree {
	return {
		tree: paths.map((path) => ({
			path,
			type: 'blob',
			mode: path === symlinkPath ? '120000' : '100644',
			sha: path === symlinkPath ? 'linksha' : `sha-${path}`
		})),
		truncated: false
	};
}

const noRead = (): Promise<string | null> => Promise.resolve(null);

describe('raster references from declared drawables', () => {
	it('reads a bitmap wrapper pointing at a mipmap', () => {
		const refs = readRasterReferences(
			`<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@mipmap/ic_key_attestation" />`
		);

		expect(refs).toEqual([{ kind: 'mipmap', name: 'ic_key_attestation' }]);
	});

	it('reads adaptive layers that point at rasters', () => {
		const refs = readRasterReferences(
			`<adaptive-icon><background android:drawable="@mipmap/ic_bg" /><foreground android:drawable="@mipmap/ic_fg" /></adaptive-icon>`
		);

		expect(refs.map((r) => r.name)).toEqual(['ic_bg', 'ic_fg']);
	});

	it('resolves a reference to the highest density raster', () => {
		const found = findRasterForReference(
			tree([
				'app/src/main/res/mipmap-hdpi/ic_key_attestation.png',
				'app/src/main/res/mipmap-xxxhdpi/ic_key_attestation.png'
			]),
			{ kind: 'mipmap', name: 'ic_key_attestation' }
		);

		expect(found).toBe('app/src/main/res/mipmap-xxxhdpi/ic_key_attestation.png');
	});

	it('does not match a reference in the wrong resource kind', () => {
		const found = findRasterForReference(tree(['app/src/main/res/drawable-hdpi/logo.png']), {
			kind: 'mipmap',
			name: 'logo'
		});

		expect(found).toBeNull();
	});
});

describe('manifest declared icons', () => {
	it('reads the application icon, not activity icons', () => {
		const manifest = `<manifest><application android:icon="@mipmap/tb_launcher" android:label="x">
			<activity android:icon="@drawable/tb_allapps" />
		</application></manifest>`;

		expect(readManifestIcon(manifest)).toEqual({ kind: 'mipmap', name: 'tb_launcher' });
	});

	it('reads a drawable icon outside any mipmap directory', () => {
		const manifest = `<manifest><application android:icon="@drawable/ic_app_icon" /></manifest>`;

		expect(readManifestIcon(manifest)).toEqual({ kind: 'drawable', name: 'ic_app_icon' });
	});

	it('prefers icon over roundIcon', () => {
		const manifest = `<manifest><application android:roundIcon="@mipmap/round" android:icon="@mipmap/square" /></manifest>`;

		expect(readManifestIcon(manifest)?.name).toBe('square');
	});

	it('falls back to roundIcon when icon is absent', () => {
		const manifest = `<manifest><application android:roundIcon="@mipmap/round" /></manifest>`;

		expect(readManifestIcon(manifest)?.name).toBe('round');
	});

	it('ignores an icon declared only on an activity', () => {
		const manifest = `<manifest><application android:label="x">
			<activity android:icon="@drawable/tb_allapps" />
		</application></manifest>`;

		expect(readManifestIcon(manifest)).toBeNull();
	});

	it('locates the declared resource across densities', () => {
		const found = findDeclaredIconPaths(
			tree([
				'app/src/main/res/drawable/ic_app_icon.xml',
				'app/src/main/res/mipmap-hdpi/ic_app_icon.png',
				'app/src/main/res/drawable/unrelated.xml'
			]),
			{ kind: 'drawable', name: 'ic_app_icon' }
		);

		expect(found.xml).toEqual(['app/src/main/res/drawable/ic_app_icon.xml']);
	});

	it('prefers the main source set manifest', () => {
		const found = findManifestPath(
			tree(['app/src/debug/AndroidManifest.xml', 'app/src/main/AndroidManifest.xml'])
		);

		expect(found).toBe('app/src/main/AndroidManifest.xml');
	});
});

describe('prefixed launcher stems', () => {
	it('finds a launcher icon behind a project prefix', () => {
		expect(findIconPath(tree(['app/src/nonlib/res/mipmap-xxxhdpi/tb_launcher.png']))).toBe(
			'app/src/nonlib/res/mipmap-xxxhdpi/tb_launcher.png'
		);
	});

	it('finds a prefixed adaptive launcher', () => {
		expect(findAdaptiveIconPath(tree(['app/src/nonlib/res/mipmap-anydpi-v26/tb_launcher.xml']))).toBe(
			'app/src/nonlib/res/mipmap-anydpi-v26/tb_launcher.xml'
		);
	});

	it('still rejects unrelated mipmap rasters', () => {
		expect(findIconPath(tree(['app/src/nonlib/res/mipmap-xxxhdpi/tb_freeform_mode.png']))).toBeNull();
	});

	it('does not match a stem that merely contains launcher', () => {
		expect(findIconPath(tree(['app/src/main/res/mipmap-xxxhdpi/launcher_banner.png']))).toBeNull();
	});
});

describe('metadata icon layouts', () => {
	it('finds a fastlane icon without the fastlane prefix or a locale', () => {
		expect(findIconPath(tree(['metadata/images/icon.png']))).toBe('metadata/images/icon.png');
	});

	it('finds a fastlane icon under an android directory without a locale', () => {
		expect(findIconPath(tree(['metadata/android/images/icon.png']))).toBe(
			'metadata/android/images/icon.png'
		);
	});

	it('prefers english when several locales ship an icon', () => {
		const found = findIconPath(
			tree([
				'fastlane/metadata/android/de-DE/images/icon.png',
				'fastlane/metadata/android/en-US/images/icon.png',
				'fastlane/metadata/android/fr-FR/images/icon.png'
			])
		);

		expect(found).toBe('fastlane/metadata/android/en-US/images/icon.png');
	});

	it('prefers a metadata icon over a mipmap raster', () => {
		const found = findIconPath(
			tree(['app/src/main/res/mipmap-xxxhdpi/ic_launcher.png', 'metadata/images/icon.png'])
		);

		expect(found).toBe('metadata/images/icon.png');
	});
});

describe('findIconPath', () => {
	it('prefers the highest density available', () => {
		const found = findIconPath(
			tree([
				'app/src/main/res/mipmap-mdpi/ic_launcher.png',
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher.png',
				'app/src/main/res/mipmap-hdpi/ic_launcher.png'
			])
		);

		expect(found).toBe('app/src/main/res/mipmap-xxxhdpi/ic_launcher.png');
	});

	it('accepts webp launcher icons, which android studio now generates by default', () => {
		const found = findIconPath(
			tree([
				'app/src/main/res/mipmap-hdpi/ic_launcher.webp',
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp'
			])
		);

		expect(found).toBe('app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp');
	});

	it('prefers the fastlane store icon over per-density mipmaps', () => {
		const found = findIconPath(
			tree([
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp',
				'fastlane/metadata/android/en-US/images/icon.png'
			])
		);

		expect(found).toBe('fastlane/metadata/android/en-US/images/icon.png');
	});

	it('falls back to the play store icon when there is no fastlane metadata', () => {
		const found = findIconPath(
			tree([
				'app/src/main/ic_launcher-playstore.png',
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp'
			])
		);

		expect(found).toBe('app/src/main/ic_launcher-playstore.png');
	});

	it('prefers the main flavour over nightly and other non-release flavours', () => {
		const found = findIconPath(
			tree([
				'app/src/nightly/res/mipmap-xxxhdpi/ic_launcher_foreground.webp',
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp'
			])
		);

		expect(found).toBe('app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp');
	});

	it('prefers a full launcher icon over a bare foreground layer', () => {
		const found = findIconPath(
			tree([
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp',
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp'
			])
		);

		expect(found).toBe('app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp');
	});

	it('ignores non-launcher mipmaps such as tv banners', () => {
		const found = findIconPath(tree(['app/src/main/res/mipmap-xxxhdpi/ic_banner.png']));

		expect(found).toBeNull();
	});

	it('returns null for adaptive-icon xml, which browsers cannot render', () => {
		const found = findIconPath(tree(['app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml']));

		expect(found).toBeNull();
	});

	it('returns null when the project ships no launcher icon', () => {
		expect(findIconPath(tree(['README.md', 'app/build.gradle.kts']))).toBeNull();
	});
});

describe('buildIconUrl', () => {
	it('builds a raw url for the icon it found', async () => {
		const url = await buildIconUrl(
			tree(['app/src/main/res/mipmap-xhdpi/ic_launcher.png']),
			'acme',
			'app',
			'main',
			noRead
		);

		expect(url).toBe(
			'https://raw.githubusercontent.com/acme/app/main/app/src/main/res/mipmap-xhdpi/ic_launcher.png'
		);
	});

	it('resolves the wgtunnel layout to its fastlane icon on the master branch', async () => {
		const url = await buildIconUrl(
			tree([
				'app/src/main/ic_launcher-playstore.png',
				'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
				'app/src/main/res/mipmap-xxxhdpi/ic_banner.png',
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp',
				'app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp',
				'app/src/nightly/res/mipmap-xxxhdpi/ic_launcher_foreground.webp',
				'fastlane/metadata/android/en-US/images/icon.png'
			]),
			'wgtunnel',
			'android',
			'master',
			noRead
		);

		expect(url).toBe(
			'https://raw.githubusercontent.com/wgtunnel/android/master/fastlane/metadata/android/en-US/images/icon.png'
		);
	});

	it('percent-encodes path segments so unusual directory names stay fetchable', async () => {
		const url = await buildIconUrl(
			tree(['My App/src/main/res/mipmap-xhdpi/ic_launcher.png']),
			'acme',
			'app',
			'main',
			noRead
		);

		expect(url).toBe(
			'https://raw.githubusercontent.com/acme/app/main/My%20App/src/main/res/mipmap-xhdpi/ic_launcher.png'
		);
	});

	it('returns null rather than substituting an owner avatar', async () => {
		await expect(buildIconUrl(tree([]), 'acme', 'app', 'main', noRead)).resolves.toBeNull();
	});

	it('follows a symlinked fastlane icon to the blob it points at', async () => {
		const symlink = 'fastlane/metadata/android/en-US/images/icon.png';
		const url = await buildIconUrl(
			treeWithSymlink(['.github/assets/logo.png', symlink], symlink),
			'ryacub',
			'rayniyomi',
			'main',
			() => Promise.resolve('../../../../../.github/assets/logo.png')
		);

		expect(url).toBe(
			'https://raw.githubusercontent.com/ryacub/rayniyomi/main/.github/assets/logo.png'
		);
	});

	it('returns null when a symlink points outside the tree', async () => {
		const symlink = 'fastlane/metadata/android/en-US/images/icon.png';
		const url = await buildIconUrl(treeWithSymlink([symlink], symlink), 'acme', 'app', 'main', () =>
			Promise.resolve('../../../../../../elsewhere/logo.png')
		);

		expect(url).toBeNull();
	});

	it('returns null when a symlink cannot be read', async () => {
		const symlink = 'fastlane/metadata/android/en-US/images/icon.png';
		const url = await buildIconUrl(
			treeWithSymlink([symlink], symlink),
			'acme',
			'app',
			'main',
			noRead
		);

		expect(url).toBeNull();
	});

	it('does not read files when the icon is a regular blob', async () => {
		let reads = 0;
		const url = await buildIconUrl(
			tree(['app/src/main/res/mipmap-xhdpi/ic_launcher.png']),
			'acme',
			'app',
			'main',
			() => {
				reads += 1;
				return Promise.resolve(null);
			}
		);

		expect(url).not.toBeNull();
		expect(reads).toBe(0);
	});
});

describe('git lfs icons', () => {
	const POINTER =
		'version https://git-lfs.github.com/spec/v1\noid sha256:c36ea4212b2db24c3adaf8697d169ad4730d152e8739be52f755068148132573\nsize 24116\n';

	function sizedTree(path: string, size: number): GithubTree {
		return {
			tree: [{ path, type: 'blob', mode: '100644', sha: 'blobsha', size }],
			truncated: false
		};
	}

	it('serves lfs-backed icons from the media host', async () => {
		const url = await buildIconUrl(
			sizedTree('fastlane/metadata/android/en-US/images/icon.png', 130),
			'acme',
			'app',
			'main',
			() => Promise.resolve(POINTER)
		);

		expect(url).toBe(
			'https://media.githubusercontent.com/media/acme/app/main/fastlane/metadata/android/en-US/images/icon.png'
		);
	});

	it('leaves a normal small file on the raw host', async () => {
		const url = await buildIconUrl(
			sizedTree('metadata/images/icon.png', 130),
			'acme',
			'app',
			'main',
			() => Promise.resolve('not a pointer')
		);

		expect(url).toBe('https://raw.githubusercontent.com/acme/app/main/metadata/images/icon.png');
	});

	it('does not read blobs for icons that are too large to be pointers', async () => {
		let reads = 0;
		const url = await buildIconUrl(
			sizedTree('metadata/images/icon.png', 24116),
			'acme',
			'app',
			'main',
			() => {
				reads += 1;
				return Promise.resolve(POINTER);
			}
		);

		expect(reads).toBe(0);
		expect(url).toBe('https://raw.githubusercontent.com/acme/app/main/metadata/images/icon.png');
	});
});

describe('resolveRelativePath', () => {
	it('resolves parent segments against the symlink directory', () => {
		expect(
			resolveRelativePath(
				'fastlane/metadata/android/en-US/images/icon.png',
				'../../../../../.github/assets/logo.png'
			)
		).toBe('.github/assets/logo.png');
	});

	it('resolves a sibling target', () => {
		expect(resolveRelativePath('app/res/icon.png', 'logo.png')).toBe('app/res/logo.png');
	});

	it('ignores redundant current-directory segments', () => {
		expect(resolveRelativePath('app/res/icon.png', './logo.png')).toBe('app/res/logo.png');
	});

	it('returns null when the target escapes the repository root', () => {
		expect(resolveRelativePath('icon.png', '../logo.png')).toBeNull();
	});
});

const ADAPTIVE = `<adaptive-icon><background android:drawable="@color/launcher_background" /><foreground android:drawable="@drawable/launcher_foreground" /></adaptive-icon>`;
const VECTOR = `<vector android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="@color/launcher_tint" android:pathData="M10,10h20v20h-20z" /></vector>`;
const COLORS = `<resources><color name="launcher_background">#FBFCFD</color><color name="launcher_tint">#0A0C10</color></resources>`;

describe('findAdaptiveIconPath', () => {
	it('finds the adaptive icon that raster lookup skips', () => {
		const found = findAdaptiveIconPath(
			tree([
				'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
				'app/src/main/res/drawable/ic_launcher_foreground.xml'
			])
		);

		expect(found).toBe('app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml');
	});

	it('accepts the bare launcher naming some projects use', () => {
		const found = findAdaptiveIconPath(tree(['app/src/main/res/mipmap-anydpi-v26/launcher.xml']));

		expect(found).toBe('app/src/main/res/mipmap-anydpi-v26/launcher.xml');
	});

	it('prefers the main flavour over nightly', () => {
		const found = findAdaptiveIconPath(
			tree([
				'app/src/nightly/res/mipmap-anydpi-v26/ic_launcher.xml',
				'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml'
			])
		);

		expect(found).toBe('app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml');
	});
});

describe('buildVectorIcon', () => {
	const files = new Map([
		['app/src/main/res/mipmap-anydpi-v26/launcher.xml', ADAPTIVE],
		['app/src/main/res/drawable/launcher_foreground.xml', VECTOR],
		['app/src/main/res/values/colors.xml', COLORS]
	]);

	const layout = tree([...files.keys()]);
	const read = (path: string) => Promise.resolve(files.get(path) ?? null);

	it('renders a vector-only icon as an inline svg data uri', async () => {
		const icon = await buildVectorIcon(layout, read);

		expect(icon).not.toBeNull();
		expect(icon?.startsWith('data:image/svg+xml;base64,')).toBe(true);
	});

	it('resolves colour references from the values directory', async () => {
		const icon = await buildVectorIcon(layout, read);
		const svg = Buffer.from(icon?.split(',')[1] ?? '', 'base64').toString('utf8');

		expect(svg).toContain('#FBFCFD');
		expect(svg).toContain('#0A0C10');
	});

	it('prefers the baseline values directory over qualified variants', async () => {
		const qualified = new Map([
			['app/src/main/res/mipmap-anydpi-v26/launcher.xml', ADAPTIVE],
			['app/src/main/res/drawable/launcher_foreground.xml', VECTOR],
			[
				'app/src/main/res/values-night-v31/colors.xml',
				`<resources><color name="launcher_background">@android:color/system_neutral1_800</color><color name="launcher_tint">@android:color/system_accent1_100</color></resources>`
			],
			[
				'app/src/main/res/values-v31/colors.xml',
				`<resources><color name="launcher_background">@android:color/system_accent1_100</color><color name="launcher_tint">@android:color/system_neutral2_700</color></resources>`
			],
			['app/src/main/res/values/colors.xml', COLORS]
		]);

		const icon = await buildVectorIcon(tree([...qualified.keys()]), (path) =>
			Promise.resolve(qualified.get(path) ?? null)
		);
		const svg = Buffer.from(icon?.split(',')[1] ?? '', 'base64').toString('utf8');

		expect(svg).toContain('#FBFCFD');
		expect(svg).toContain('#0A0C10');
	});

	it('falls back to the night variant when there is no baseline', async () => {
		const nightOnly = new Map([
			['app/src/main/res/mipmap-anydpi-v26/launcher.xml', ADAPTIVE],
			['app/src/main/res/drawable/launcher_foreground.xml', VECTOR],
			['app/src/main/res/values-night/colors.xml', COLORS]
		]);

		const icon = await buildVectorIcon(tree([...nightOnly.keys()]), (path) =>
			Promise.resolve(nightOnly.get(path) ?? null)
		);
		const svg = Buffer.from(icon?.split(',')[1] ?? '', 'base64').toString('utf8');

		expect(svg).toContain('#FBFCFD');
	});

	it('returns null when the project ships no adaptive icon', async () => {
		expect(await buildVectorIcon(tree(['README.md']), read)).toBeNull();
	});

	it('returns null when the foreground drawable is missing', async () => {
		const icon = await buildVectorIcon(
			tree(['app/src/main/res/mipmap-anydpi-v26/launcher.xml']),
			(path) => Promise.resolve(path.endsWith('launcher.xml') ? ADAPTIVE : null)
		);

		expect(icon).toBeNull();
	});
});
