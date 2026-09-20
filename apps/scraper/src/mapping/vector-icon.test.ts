import { describe, expect, it } from 'vitest';
import { parseAdaptiveIcon, parseColors, vectorToSvg } from './vector-icon.ts';
import { composeAdaptiveSvg, toDataUri } from './adaptive-svg.ts';

const FOREGROUND = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
	android:width="108dp" android:height="108dp"
	android:viewportWidth="256" android:viewportHeight="256">
	<group android:scaleX="0.48" android:scaleY="0.48" android:translateX="66.56" android:translateY="66.56">
		<path android:fillColor="@color/launcher_tint" android:pathData="M10,10h20v20h-20z" />
	</group>
</vector>`;

describe('parseColors', () => {
	it('reads named colours out of a values file', () => {
		const colors = parseColors(
			`<resources><color name="launcher_tint">#0A0C10</color><color name="bg">#FBFCFD</color></resources>`
		);

		expect(colors.get('launcher_tint')).toBe('#0A0C10');
		expect(colors.get('bg')).toBe('#FBFCFD');
	});

	it('keeps colour references so chains can be followed', () => {
		const colors = parseColors(`<resources><color name="ref">@color/other</color></resources>`);

		expect(colors.get('ref')).toBe('@color/other');
	});

	it('ignores values that are neither literals nor colour references', () => {
		const colors = parseColors(
			`<resources><color name="ref">?attr/colorPrimary</color></resources>`
		);

		expect(colors.has('ref')).toBe(false);
	});
});

describe('parseAdaptiveIcon', () => {
	it('reads colour backgrounds and drawable foregrounds', () => {
		const refs = parseAdaptiveIcon(
			`<adaptive-icon><background android:drawable="@color/launcher_background" /><foreground android:drawable="@drawable/launcher_foreground" /></adaptive-icon>`
		);

		expect(refs.background).toEqual({ kind: 'color', name: 'launcher_background' });
		expect(refs.foreground).toEqual({ kind: 'drawable', name: 'launcher_foreground' });
	});

	it('ignores mipmap references, which point at raster assets', () => {
		const refs = parseAdaptiveIcon(
			`<adaptive-icon><foreground android:drawable="@mipmap/ic_launcher_foreground" /></adaptive-icon>`
		);

		expect(refs.foreground).toBeNull();
	});
});

describe('vectorToSvg', () => {
	it('converts android path data and resolves colour references', () => {
		const svg = vectorToSvg(FOREGROUND, new Map([['launcher_tint', '#0A0C10']]));

		expect(svg).toContain('viewBox="0 0 256 256"');
		expect(svg).toContain('fill="#0A0C10"');
		expect(svg).toContain('d="M10,10h20v20h-20z"');
	});

	it('carries group transforms across as an svg transform', () => {
		const svg = vectorToSvg(FOREGROUND, new Map());

		expect(svg).toContain('transform="translate(66.56 66.56) scale(0.48 0.48)"');
	});

	it('converts stroked paths without a fill', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="108" android:viewportHeight="108"><path android:pathData="M54,34 L44,52" android:strokeWidth="2" android:strokeColor="#FFFFFF" android:strokeLineCap="round" /></vector>`,
			new Map()
		);

		expect(svg).toContain('stroke="#FFFFFF"');
		expect(svg).toContain('stroke-width="2"');
		expect(svg).toContain('stroke-linecap="round"');
		expect(svg).toContain('fill="none"');
	});

	it('reorders android argb hex into svg rgba', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="108" android:viewportHeight="108"><path android:pathData="M0,0h1v1h-1z" android:fillColor="#80FF0000" /></vector>`,
			new Map()
		);

		expect(svg).toContain('fill="#FF000080"');
	});

	it('returns null when the vector has no usable paths', () => {
		expect(vectorToSvg(`<vector android:viewportWidth="108"></vector>`, new Map())).toBeNull();
	});

	it('returns null for input that is not a vector drawable', () => {
		expect(vectorToSvg(`<adaptive-icon />`, new Map())).toBeNull();
	});
});

describe('colour references', () => {
	it('follows a chain of colour references to a literal', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="@color/icon_background" android:pathData="M0,0h24v24h-24z" /></vector>`,
			new Map([
				['icon_background', '@color/primary'],
				['primary', '#F2E672']
			])
		);

		expect(svg).toContain('fill="#F2E672"');
	});

	it('resolves fixed framework colours', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="@android:color/white" android:pathData="M0,0h24v24h-24z" /></vector>`,
			new Map()
		);

		expect(svg).toContain('fill="#FFFFFFFF"');
	});

	it('refuses runtime material you colours rather than guessing', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="@android:color/system_accent1_100" android:pathData="M0,0h24v24h-24z" /></vector>`,
			new Map()
		);

		expect(svg).toContain('fill="#000000"');
	});

	it('does not loop on a cyclic reference', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="@color/a" android:pathData="M0,0h24v24h-24z" /></vector>`,
			new Map([
				['a', '@color/b'],
				['b', '@color/a']
			])
		);

		expect(svg).toContain('fill="#000000"');
	});
});

describe('invisible paths', () => {
	it('skips a path that declares neither fill nor stroke', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M4,4h16v16h-16z" /><path android:pathData="M0 0h24v24H0z" /></vector>`,
			new Map()
		);

		expect(svg).not.toContain('#000000');
		expect(svg).toContain('#FFFFFF');
	});
});

describe('framework colour layers', () => {
	it('parses a background pointing at an android framework colour', () => {
		const refs = parseAdaptiveIcon(
			`<adaptive-icon><background android:drawable="@android:color/holo_blue_dark" /><foreground android:drawable="@drawable/fg" /></adaptive-icon>`
		);

		expect(refs.background).toEqual({ kind: 'color', name: 'android:holo_blue_dark' });
	});
});

describe('fill rules', () => {
	it('carries evenOdd through so knocked-out shapes stay hollow', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:fillType="evenOdd" android:pathData="M0,0h24v24h-24z" /></vector>`,
			new Map()
		);

		expect(svg).toContain('fill-rule="evenodd"');
	});

	it('leaves the default winding rule alone', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M0,0h24v24h-24z" /></vector>`,
			new Map()
		);

		expect(svg).not.toContain('fill-rule');
	});
});

describe('group transforms', () => {
	it('scales about the declared pivot', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="24" android:viewportHeight="24"><group android:pivotX="12" android:pivotY="12" android:scaleX="0.6" android:scaleY="0.6"><path android:fillColor="#FF0000" android:pathData="M0,0h24v24h-24z" /></group></vector>`,
			new Map()
		);

		expect(svg).toContain('transform="translate(12 12) scale(0.6 0.6) translate(-12 -12)"');
	});

	it('applies rotation before scale, matching android', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="24" android:viewportHeight="24"><group android:pivotX="12" android:pivotY="12" android:rotation="90" android:scaleX="0.5" android:scaleY="0.5"><path android:fillColor="#FF0000" android:pathData="M0,0h24v24h-24z" /></group></vector>`,
			new Map()
		);

		expect(svg).toContain('rotate(90) scale(0.5 0.5)');
	});
});

describe('composeAdaptiveSvg', () => {
	it('normalises layers drawn in different viewports onto one canvas', () => {
		const svg = composeAdaptiveSvg({
			background: {
				kind: 'vector',
				value: `<vector android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#4D5DBA" android:pathData="M0,0h108v108h-108z" /></vector>`
			},
			foreground: `<vector android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M4,4h16v16h-16z" /></vector>`,
			colors: new Map()
		});

		expect(svg).toContain('viewBox="18 18 72 72"');
		expect(svg).toContain('<g transform="scale(4.5 4.5)">');
		expect(svg).not.toContain('scale(1 1)');
	});

	it('paints a colour background beneath the foreground', () => {
		const svg = composeAdaptiveSvg({
			background: { kind: 'color', value: '@color/launcher_background' },
			foreground: FOREGROUND,
			colors: new Map([
				['launcher_background', '#FBFCFD'],
				['launcher_tint', '#0A0C10']
			])
		});

		expect(svg).toContain('<rect width="108" height="108" fill="#FBFCFD"/>');
		expect(svg).toContain('fill="#0A0C10"');
	});

	it('clips to the safe zone so the icon matches launcher framing', () => {
		const svg = composeAdaptiveSvg({
			background: null,
			foreground: FOREGROUND,
			colors: new Map()
		});

		expect(svg).toContain('clipPath');
		expect(svg).toContain('clip-path="url(#c)"');
	});

	it('returns null when the foreground cannot be converted', () => {
		expect(
			composeAdaptiveSvg({ background: null, foreground: '<vector />', colors: new Map() })
		).toBeNull();
	});
});

describe('toDataUri', () => {
	it('produces a base64 source both browsers and android image loaders accept', () => {
		const svg = '<svg xmlns="http://www.w3.org/2000/svg"><path d="M0,0"/></svg>';

		const uri = toDataUri(svg);

		expect(uri.startsWith('data:image/svg+xml;base64,')).toBe(true);
		expect(uri).not.toContain('#');
	});

	it('round-trips the svg it encodes', () => {
		const svg = '<svg xmlns="http://www.w3.org/2000/svg"><path d="M0,0" fill="#ff0000"/></svg>';

		const encoded = toDataUri(svg).slice('data:image/svg+xml;base64,'.length);

		expect(Buffer.from(encoded, 'base64').toString('utf8')).toBe(svg);
	});
});
