import { describe, expect, it } from 'vitest';
import { buildVectorIcon } from './adaptive-vector.ts';
import type { GithubTree } from '@yuki/github';

function tree(paths: string[]): GithubTree {
	return { tree: paths.map((path) => ({ path, type: 'blob' })), truncated: false };
}

function readerFor(files: Map<string, string>) {
	return (path: string) => Promise.resolve(files.get(path) ?? null);
}

function decode(uri: string): string {
	return Buffer.from(uri.split(',')[1] ?? '', 'base64').toString('utf8');
}

const COLORS = `<resources><color name="launcher_tint">#0A0C10</color></resources>`;

describe('layer-list foregrounds', () => {
	it('flags a layer-list whose art is a raster the vector renderer cannot draw', async () => {
		const files = new Map([
			[
				'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
				`<adaptive-icon><background android:drawable="@mipmap/ic_launcher_background" /><foreground android:drawable="@drawable/ic_launcher_foreground" /></adaptive-icon>`
			],
			[
				'app/src/main/res/drawable/ic_launcher_foreground.xml',
				`<layer-list><item android:drawable="@mipmap/ic_launcher_foreground_base" /><item><vector android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#B3FFFFFF" android:pathData="M69,35h3v5h5v3h-5v5h-3z" /></vector></item></layer-list>`
			],
			['app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground_base.png', ''],
			['app/src/main/res/mipmap-xxxhdpi/ic_launcher_background.png', '']
		]);

		const result = await buildVectorIcon(tree([...files.keys()]), readerFor(files));

		expect(result).not.toBeNull();
		expect(result?.fidelity.unresolved).toBe(true);
		expect(result?.fidelity.reasons).toContain('layer-reference:mipmap');
		expect(result?.fidelity.reasons).toContain('background-reference:mipmap');
	});

	it('composes a layer-list whose layers are all vectors', async () => {
		const files = new Map([
			[
				'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
				`<adaptive-icon><background android:drawable="@color/launcher_tint" /><foreground android:drawable="@drawable/ic_launcher_foreground" /></adaptive-icon>`
			],
			[
				'app/src/main/res/drawable/ic_launcher_foreground.xml',
				`<layer-list><item android:drawable="@drawable/base_art" /><item><vector android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#BADA55" android:pathData="M69,35h3v5h5v3h-5v5h-3z" /></vector></item></layer-list>`
			],
			[
				'app/src/main/res/drawable/base_art.xml',
				`<vector android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#123456" android:pathData="M0,0h108v108h-108z" /></vector>`
			],
			['app/src/main/res/values/colors.xml', COLORS]
		]);

		const result = await buildVectorIcon(tree([...files.keys()]), readerFor(files));

		expect(result).not.toBeNull();
		expect(result?.fidelity.unresolved).toBe(false);

		const svg = decode(result?.svg ?? '');
		expect(svg).toContain('#123456');
		expect(svg).toContain('#BADA55');
	});

	it('rescales a nested layer drawn on a different viewport', async () => {
		const files = new Map([
			[
				'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
				`<adaptive-icon><foreground android:drawable="@drawable/ic_launcher_foreground" /></adaptive-icon>`
			],
			[
				'app/src/main/res/drawable/ic_launcher_foreground.xml',
				`<layer-list><item android:drawable="@drawable/base_art" /><item><vector android:viewportWidth="54" android:viewportHeight="54"><path android:fillColor="#BADA55" android:pathData="M0,0h10v10h-10z" /></vector></item></layer-list>`
			],
			[
				'app/src/main/res/drawable/base_art.xml',
				`<vector android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#123456" android:pathData="M0,0h108v108h-108z" /></vector>`
			]
		]);

		const result = await buildVectorIcon(tree([...files.keys()]), readerFor(files));
		const svg = decode(result?.svg ?? '');

		expect(result?.fidelity.unresolved).toBe(false);
		expect(svg).toContain('scale(2 2)');
	});

	it('flags a root drawable kind the renderer cannot draw at all', async () => {
		const files = new Map([
			[
				'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
				`<adaptive-icon><foreground android:drawable="@drawable/ic_launcher_foreground" /></adaptive-icon>`
			],
			[
				'app/src/main/res/drawable/ic_launcher_foreground.xml',
				`<bitmap android:src="@mipmap/ic_launcher_foreground_base" />`
			]
		]);

		const result = await buildVectorIcon(tree([...files.keys()]), readerFor(files));

		expect(result).toBeNull();
	});

	it('does not render a bare nested vector as if it were the whole drawable', async () => {
		const files = new Map([
			[
				'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
				`<adaptive-icon><foreground android:drawable="@drawable/ic_launcher_foreground" /></adaptive-icon>`
			],
			[
				'app/src/main/res/drawable/ic_launcher_foreground.xml',
				`<layer-list><item android:drawable="@mipmap/art" /></layer-list>`
			],
			['app/src/main/res/mipmap-xxxhdpi/art.png', '']
		]);

		const result = await buildVectorIcon(tree([...files.keys()]), readerFor(files));

		expect(result).toBeNull();
	});
});
