import { describe, expect, it } from 'vitest';
import { buildIconUrl, findIconPath } from './icon.ts';
import type { GithubTree } from '../github/types.ts';

function tree(paths: string[], truncated = false): GithubTree {
	return {
		tree: paths.map((path) => ({ path, type: 'blob' })),
		truncated
	};
}

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

	it('returns null for adaptive-icon xml, which browsers cannot render', () => {
		const found = findIconPath(tree(['app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml']));

		expect(found).toBeNull();
	});

	it('returns null when the project ships no launcher icon', () => {
		expect(findIconPath(tree(['README.md', 'app/build.gradle.kts']))).toBeNull();
	});
});

describe('buildIconUrl', () => {
	it('builds a raw url for the icon it found', () => {
		const url = buildIconUrl(
			tree(['app/src/main/res/mipmap-xhdpi/ic_launcher.png']),
			'acme',
			'app',
			'main'
		);

		expect(url).toBe(
			'https://raw.githubusercontent.com/acme/app/main/app/src/main/res/mipmap-xhdpi/ic_launcher.png'
		);
	});

	it('returns null rather than substituting an owner avatar', () => {
		expect(buildIconUrl(tree([]), 'acme', 'app', 'main')).toBeNull();
	});
});
