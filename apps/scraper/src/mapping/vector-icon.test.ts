import { describe, expect, it } from 'vitest';
import {
	composeAdaptiveSvg,
	parseAdaptiveIcon,
	parseColors,
	toDataUri,
	vectorToSvg
} from './vector-icon.ts';

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

	it('ignores theme references that are not literal colours', () => {
		const colors = parseColors(`<resources><color name="ref">@color/other</color></resources>`);

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

describe('composeAdaptiveSvg', () => {
	it('paints a colour background beneath the foreground', () => {
		const svg = composeAdaptiveSvg({
			background: { kind: 'color', value: '@color/launcher_background' },
			foreground: FOREGROUND,
			colors: new Map([
				['launcher_background', '#FBFCFD'],
				['launcher_tint', '#0A0C10']
			])
		});

		expect(svg).toContain('<rect width="256" height="256" fill="#FBFCFD"/>');
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
