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

const PNG = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
const PNG_URI = `data:image/png;base64,${PNG.toString('base64')}`;

function downloaderFor(rasters: Map<string, Buffer>) {
	return (path: string) => Promise.resolve(rasters.get(path) ?? null);
}

describe('inset drawables', () => {
	const files = new Map([
		[
			'app/src/main/res/drawable/ic_app_icon.xml',
			`<adaptive-icon><background android:drawable="@android:color/holo_blue_dark" /><foreground android:drawable="@drawable/app_icon_foreground" /></adaptive-icon>`
		],
		[
			'app/src/main/res/drawable/app_icon_foreground.xml',
			`<inset android:insetLeft="25%" android:insetTop="25%" android:insetRight="25%" android:insetBottom="25%"><vector android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="@android:color/white" android:pathData="M5,19h14v1H5z" /></vector></inset>`
		]
	]);

	it('renders an inline vector shrunk into the inset bounds', async () => {
		const result = await buildVectorIcon(tree([...files.keys()]), readerFor(files), {
			declaredPath: 'app/src/main/res/drawable/ic_app_icon.xml'
		});

		expect(result?.fidelity.unresolved).toBe(false);
		const svg = decode(result?.svg ?? '');
		expect(svg).toContain('fill="#0099CCFF"');
		expect(svg).toContain('translate(6 6)');
		expect(svg).toContain('scale(0.5 0.5)');
	});

	it('resolves an inset that wraps a referenced drawable', async () => {
		const referenced = new Map([
			...files,
			[
				'app/src/main/res/drawable/app_icon_foreground.xml',
				`<inset android:drawable="@drawable/glyph" android:inset="10%" />`
			],
			[
				'app/src/main/res/drawable/glyph.xml',
				`<vector android:viewportWidth="100" android:viewportHeight="100"><path android:fillColor="#BADA55" android:pathData="M0,0h100v100H0z" /></vector>`
			]
		]);

		const result = await buildVectorIcon(tree([...referenced.keys()]), readerFor(referenced), {
			declaredPath: 'app/src/main/res/drawable/ic_app_icon.xml'
		});

		const svg = decode(result?.svg ?? '');
		expect(svg).toContain('#BADA55');
		expect(svg).toContain('translate(10 10)');
		expect(svg).toContain('scale(0.8 0.8)');
	});
});

describe('raster layers', () => {
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
	const rasters = new Map([
		['app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground_base.png', PNG],
		['app/src/main/res/mipmap-xxxhdpi/ic_launcher_background.png', PNG]
	]);

	it('embeds raster layers beneath the vector art drawn over them', async () => {
		const result = await buildVectorIcon(tree([...files.keys()]), readerFor(files), {
			download: downloaderFor(rasters)
		});

		expect(result?.fidelity.unresolved).toBe(false);
		const svg = decode(result?.svg ?? '');
		expect(svg.match(/<image /g)).toHaveLength(2);
		expect(svg).toContain(`xlink:href="${PNG_URI}"`);
		expect(svg.lastIndexOf('<image ')).toBeLessThan(svg.indexOf('<path '));
	});

	it('still flags raster layers when nothing can download them', async () => {
		const result = await buildVectorIcon(tree([...files.keys()]), readerFor(files));

		expect(result?.fidelity.unresolved).toBe(true);
		expect(result?.fidelity.reasons).toContain('layer-reference:mipmap');
	});

	it('flags a raster layer whose download fails', async () => {
		const result = await buildVectorIcon(tree([...files.keys()]), readerFor(files), {
			download: downloaderFor(new Map())
		});

		expect(result?.fidelity.unresolved).toBe(true);
	});

	it('draws a bitmap wrapper as its raster', async () => {
		const wrapped = new Map([
			...files,
			[
				'app/src/main/res/drawable/ic_launcher_foreground.xml',
				`<bitmap android:src="@mipmap/ic_launcher_foreground_base" />`
			]
		]);

		const result = await buildVectorIcon(tree([...wrapped.keys()]), readerFor(wrapped), {
			download: downloaderFor(rasters)
		});

		expect(result?.fidelity.unresolved).toBe(false);
		expect(decode(result?.svg ?? '').match(/<image /g)).toHaveLength(2);
	});
});
