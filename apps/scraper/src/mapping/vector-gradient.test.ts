import { describe, expect, it } from 'vitest';
import { vectorToSvg } from './vector-icon.ts';

const identity = (raw: string | null) => raw;

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

	it('gives each gradient in one document a distinct id', () => {
		const doubled = GRADIENT_PATH.replace('</vector>', '') + GRADIENT_PATH.replace(/^[\s\S]*?<path/, '<path');
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
