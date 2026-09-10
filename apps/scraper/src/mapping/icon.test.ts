import { describe, expect, it } from 'vitest';
import {
	buildIconUrl,
	buildVectorIcon,
	findAdaptiveIconPath,
	findIconPath,
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
		expect(icon?.startsWith('data:image/svg+xml;charset=utf-8,')).toBe(true);
	});

	it('resolves colour references from the values directory', async () => {
		const icon = await buildVectorIcon(layout, read);
		const svg = decodeURIComponent(icon?.split(',')[1] ?? '');

		expect(svg).toContain('#FBFCFD');
		expect(svg).toContain('#0A0C10');
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
