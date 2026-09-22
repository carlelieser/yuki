import { describe, expect, it } from 'vitest';
import { vectorToSvg } from './vector-icon.ts';
import { composeAdaptiveSvg } from './adaptive-svg.ts';

const GRADIENT_PATH = `<vector xmlns:android="http://schemas.android.com/apk/res/android"
	android:viewportWidth="512" android:viewportHeight="512">
	<path android:pathData="M0,0h10v10h-10z">
		<aapt:attr name="android:fillColor">
			<gradient android:startX="83" android:startY="216" android:endX="429" android:endY="216" android:type="linear">
				<item android:color="#1E5AA8" android:offset="0" />
				<item android:color="#00AFAE" android:offset="1" />
			</gradient>
		</aapt:attr>
	</path>
</vector>`;

const RADIAL_PATH = GRADIENT_PATH.replace(
	'android:type="linear"',
	'android:type="radial" android:centerX="256" android:centerY="256" android:gradientRadius="128"'
);

const ATTRIBUTE_GRADIENT = `<vector xmlns:android="http://schemas.android.com/apk/res/android"
	android:viewportWidth="108" android:viewportHeight="108">
	<path android:pathData="M0,0h108v108h-108z">
		<aapt:attr name="android:fillColor">
			<gradient
				android:type="linear"
				android:startX="0"
				android:startY="0"
				android:endX="108"
				android:endY="108"
				android:startColor="#4C8DF0"
				android:endColor="#16233F" />
		</aapt:attr>
	</path>
</vector>`;

describe('colour resource gradients', () => {
	const COLOR_RESOURCE = `<gradient xmlns:android="http://schemas.android.com/apk/res/android"
		android:startX="5.4" android:startY="5.4" android:endX="102.6" android:endY="102.6" android:type="linear">
		<item android:color="@color/brand_start" android:offset="0.0" />
		<item android:color="@color/brand_end" android:offset="1.0" />
	</gradient>`;

	const VECTOR = `<vector android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="@color/launcher_bg" android:pathData="M0,0h108v108h-108z" /></vector>`;

	it('fills a path from a gradient declared in res/color', () => {
		const svg = vectorToSvg(
			VECTOR,
			new Map([
				['brand_start', '#E1115C'],
				['brand_end', '#8B0A3A']
			]),
			{ gradients: new Map([['launcher_bg', COLOR_RESOURCE]]) }
		);

		expect(svg).toContain('fill="url(#');
		expect(svg).toContain('stop-color="#E1115C"');
		expect(svg).toContain('stop-color="#8B0A3A"');
		expect(svg).not.toContain('#000000');
	});

	it('falls back to the flat colour when no gradient resource matches', () => {
		const svg = vectorToSvg(VECTOR, new Map([['launcher_bg', '#123456']]), {
			gradients: new Map()
		});

		expect(svg).toContain('fill="#123456"');
	});
});

describe('attribute-form gradients', () => {
	it('renders a self-closing gradient declared with start and end colours', () => {
		const svg = vectorToSvg(ATTRIBUTE_GRADIENT, new Map()) ?? '';

		expect(svg).toContain('fill="url(#');
		expect(svg).not.toContain('fill="#000000"');
	});

	it('emits the start and end colours as stops', () => {
		const svg = vectorToSvg(ATTRIBUTE_GRADIENT, new Map()) ?? '';

		expect(svg).toContain('stop-color="#4C8DF0"');
		expect(svg).toContain('stop-color="#16233F"');
	});

	it('places a centre colour midway between the ends', () => {
		const withCentre = ATTRIBUTE_GRADIENT.replace(
			'android:startColor="#4C8DF0"',
			'android:startColor="#4C8DF0" android:centerColor="#2A5580"'
		);
		const svg = vectorToSvg(withCentre, new Map()) ?? '';

		expect(svg).toContain('<stop offset="0.5" stop-color="#2A5580"/>');
	});

	it('falls back to a flat fill when the gradient has no direction', () => {
		const degenerate = ATTRIBUTE_GRADIENT.replace('android:endX="108"', 'android:endX="0"').replace(
			'android:endY="108"',
			'android:endY="0"'
		);
		const svg = vectorToSvg(degenerate, new Map()) ?? '';

		expect(svg).toContain('fill="#4C8DF0"');
		expect(svg).not.toContain('fill="#000000"');
	});

	it('still prefers item stops when both forms are present', () => {
		const both = ATTRIBUTE_GRADIENT.replace(
			'android:endColor="#16233F" />',
			`android:endColor="#16233F"><item android:color="#AAAAAA" android:offset="0" /><item android:color="#BBBBBB" android:offset="1" /></gradient>`
		);
		const svg = vectorToSvg(both, new Map()) ?? '';

		expect(svg).toContain('stop-color="#AAAAAA"');
		expect(svg).not.toContain('stop-color="#4C8DF0"');
	});
});

describe('gradient fills', () => {
	it('renders a path whose fill is a nested gradient rather than dropping it', () => {
		const svg = vectorToSvg(GRADIENT_PATH, new Map());

		expect(svg).toContain('<path');
		expect(svg).toContain('fill="url(#');
	});

	it('emits the gradient stops in order', () => {
		const svg = vectorToSvg(GRADIENT_PATH, new Map()) ?? '';

		const first = svg.indexOf('#1E5AA8');
		const second = svg.indexOf('#00AFAE');

		expect(first).toBeGreaterThan(-1);
		expect(second).toBeGreaterThan(first);
	});

	it('carries the linear gradient coordinates onto the svg element', () => {
		const svg = vectorToSvg(GRADIENT_PATH, new Map()) ?? '';

		expect(svg).toContain('<linearGradient');
		expect(svg).toContain('x1="83"');
		expect(svg).toContain('x2="429"');
	});

	it('renders a radial gradient with its centre and radius', () => {
		const svg = vectorToSvg(RADIAL_PATH, new Map()) ?? '';

		expect(svg).toContain('<radialGradient');
		expect(svg).toContain('cx="256"');
		expect(svg).toContain('r="128"');
	});

	it('produces the same svg every time it converts the same icon', () => {
		expect(vectorToSvg(GRADIENT_PATH, new Map())).toBe(vectorToSvg(GRADIENT_PATH, new Map()));
	});

	it('keeps background and foreground gradient ids apart when composed', () => {
		const composed =
			composeAdaptiveSvg({
				background: { kind: 'vector', value: GRADIENT_PATH },
				foreground: GRADIENT_PATH,
				colors: new Map()
			}) ?? '';

		const ids = [...composed.matchAll(/id="([^"]+)"/g)].map((match) => match[1]);
		const references = [...composed.matchAll(/url\(#([^)]+)\)/g)].map((match) => match[1]);

		expect(new Set(ids).size).toBe(ids.length);
		expect(references.every((reference) => ids.includes(reference))).toBe(true);
	});

	it('gives each gradient in one document a distinct id', () => {
		const doubled =
			GRADIENT_PATH.replace('</vector>', '') + GRADIENT_PATH.replace(/^[\s\S]*?<path/, '<path');
		const svg = vectorToSvg(doubled, new Map()) ?? '';

		const ids = [...svg.matchAll(/<linearGradient id="([^"]+)"/g)].map((match) => match[1]);

		expect(ids.length).toBe(2);
		expect(new Set(ids).size).toBe(2);
	});
});

describe('self-closing paths', () => {
	it('still renders paths that carry a flat fill colour', () => {
		const flat = `<vector xmlns:android="http://schemas.android.com/apk/res/android"
			android:viewportWidth="100" android:viewportHeight="100">
			<path android:fillColor="#FF0000" android:pathData="M0,0h10v10h-10z" />
		</vector>`;

		expect(vectorToSvg(flat, new Map())).toContain('fill="#FF0000"');
	});
});

describe('stroke gradients', () => {
	const STROKE_PATH = `<vector xmlns:android="http://schemas.android.com/apk/res/android"
	android:viewportWidth="108" android:viewportHeight="108">
	<path
		android:strokeWidth="13.5"
		android:strokeLineCap="round"
		android:strokeLineJoin="round"
		android:pathData="M 42,34 L 42,64 Q 42,74 52,74 L 66,74">
		<aapt:attr name="android:strokeColor">
			<gradient android:startX="42" android:startY="34" android:endX="54" android:endY="74" android:type="linear">
				<item android:offset="0.0" android:color="#FF8EED70" />
				<item android:offset="1.0" android:color="#FF38C666" />
			</gradient>
		</aapt:attr>
	</path>
</vector>`;

	it('paints an aapt gradient onto the stroke it was declared on', () => {
		const svg = vectorToSvg(STROKE_PATH, new Map());

		expect(svg).toContain('stroke="url(#g0)"');
		expect(svg).toContain('stroke-width="13.5"');
		expect(svg).toContain('stroke-linecap="round"');
	});

	it('leaves a stroke-only path unfilled', () => {
		const svg = vectorToSvg(STROKE_PATH, new Map());

		expect(svg).toContain('fill="none"');
		expect(svg).not.toContain('fill="url(#g0)"');
	});

	it('carries strokeAlpha onto the stroke it dims', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="96" android:viewportHeight="96"><path android:strokeColor="#FFFFFF" android:strokeAlpha="0.5" android:strokeWidth="5" android:pathData="M10,10h40v40h-40z" /></vector>`,
			new Map()
		);

		expect(svg).toContain('stroke-opacity="0.5"');
		expect(svg).toContain('stroke="#FFFFFF"');
	});

	it('leaves a stroke undimmed when no strokeAlpha is declared', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="96" android:viewportHeight="96"><path android:strokeColor="#FFFFFF" android:strokeWidth="5" android:pathData="M10,10h40v40h-40z" /></vector>`,
			new Map()
		);

		expect(svg).not.toContain('stroke-opacity');
	});

	it('keeps a fill gradient on the fill', () => {
		const svg = vectorToSvg(GRADIENT_PATH, new Map());

		expect(svg).toContain('fill="url(#g0)"');
		expect(svg).not.toContain('stroke="url(#g0)"');
	});

	it('paints both when a path declares a fill and a stroke', () => {
		const svg = vectorToSvg(
			`<vector android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#FFFFFF" android:strokeColor="#FF0000" android:strokeWidth="4" android:pathData="M0,0h108v108h-108z" /></vector>`,
			new Map()
		);

		expect(svg).toContain('fill="#FFFFFF"');
		expect(svg).toContain('stroke="#FF0000"');
		expect(svg).toContain('stroke-width="4"');
	});
});
