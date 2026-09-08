const ADAPTIVE_LAYER =
	/<(background|foreground)\b[^>]*android:drawable="@(color|drawable|mipmap)\/([^"]+)"/g;
const VECTOR_TAG = /<vector\b[^>]*>/;
const PATH_TAG = /<path\b[^>]*?\/>/gs;
const GROUP_TAG = /<group\b[^>]*>/;
const COLOR_ENTRY = /<color\s+name="([^"]+)"\s*>\s*([^<\s]+)\s*<\/color>/g;

const MAX_SOURCE_BYTES = 64 * 1024;
const MAX_PATHS = 64;
const CANVAS = 108;
const VIEWPORT_INSET = 18;

export type AdaptiveIconRefs = {
	background: { kind: 'color' | 'drawable'; name: string } | null;
	foreground: { kind: 'color' | 'drawable'; name: string } | null;
};

function attribute(source: string, name: string): string | null {
	const match = source.match(new RegExp(`android:${name}="([^"]*)"`));
	return match?.[1] ?? null;
}

function numeric(source: string, name: string, fallback: number): number {
	const raw = attribute(source, name);
	if (raw === null) return fallback;

	const parsed = Number.parseFloat(raw.replace(/(dp|dip|px|sp)$/, ''));
	return Number.isFinite(parsed) ? parsed : fallback;
}

export function parseColors(xml: string): Map<string, string> {
	const colors = new Map<string, string>();

	for (const match of xml.matchAll(COLOR_ENTRY)) {
		const name = match[1];
		const value = match[2];
		if (name !== undefined && value !== undefined && value.startsWith('#')) {
			colors.set(name, value);
		}
	}

	return colors;
}

export function parseAdaptiveIcon(xml: string): AdaptiveIconRefs {
	const refs: AdaptiveIconRefs = { background: null, foreground: null };

	for (const match of xml.matchAll(ADAPTIVE_LAYER)) {
		const layer = match[1];
		const kind = match[2];
		const name = match[3];
		if (name === undefined || kind === 'mipmap') continue;

		const ref = { kind: kind === 'color' ? ('color' as const) : ('drawable' as const), name };
		if (layer === 'background') refs.background = ref;
		if (layer === 'foreground') refs.foreground = ref;
	}

	return refs;
}

function resolveColor(raw: string | null, colors: Map<string, string>): string | null {
	if (raw === null) return null;

	const value = raw.startsWith('@')
		? (colors.get(raw.replace(/^@(android:)?color\//, '')) ?? null)
		: raw;

	if (value === null || !/^#(?:[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})$/i.test(value)) return null;

	if (value.length === 9) {
		const alpha = value.slice(1, 3);
		const rgb = value.slice(3);
		return `#${rgb}${alpha}`;
	}

	return value;
}

function escapeXml(value: string): string {
	return value
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;');
}

function groupTransform(vector: string): string | null {
	const group = vector.match(GROUP_TAG)?.[0];
	if (group === undefined) return null;

	const parts: string[] = [];
	const translateX = numeric(group, 'translateX', 0);
	const translateY = numeric(group, 'translateY', 0);
	const scaleX = numeric(group, 'scaleX', 1);
	const scaleY = numeric(group, 'scaleY', 1);
	const rotation = numeric(group, 'rotation', 0);

	if (translateX !== 0 || translateY !== 0) parts.push(`translate(${translateX} ${translateY})`);
	if (scaleX !== 1 || scaleY !== 1) parts.push(`scale(${scaleX} ${scaleY})`);
	if (rotation !== 0) parts.push(`rotate(${rotation})`);

	return parts.length === 0 ? null : parts.join(' ');
}

function convertPaths(vector: string, colors: Map<string, string>, fallbackFill: string): string {
	const rendered: string[] = [];

	for (const match of vector.matchAll(PATH_TAG)) {
		if (rendered.length >= MAX_PATHS) break;

		const tag = match[0];
		const data = attribute(tag, 'pathData');
		if (data === null || data.trim() === '') continue;

		const fill = resolveColor(attribute(tag, 'fillColor'), colors);
		const stroke = resolveColor(attribute(tag, 'strokeColor'), colors);
		const strokeWidth = numeric(tag, 'strokeWidth', 0);

		const attributes = [`d="${escapeXml(data.trim())}"`];
		attributes.push(`fill="${fill ?? (stroke === null ? fallbackFill : 'none')}"`);

		if (stroke !== null && strokeWidth > 0) {
			attributes.push(`stroke="${stroke}"`, `stroke-width="${strokeWidth}"`);

			const cap = attribute(tag, 'strokeLineCap');
			const join = attribute(tag, 'strokeLineJoin');
			if (cap !== null) attributes.push(`stroke-linecap="${escapeXml(cap)}"`);
			if (join !== null) attributes.push(`stroke-linejoin="${escapeXml(join)}"`);
		}

		const alpha = attribute(tag, 'fillAlpha');
		if (alpha !== null) attributes.push(`fill-opacity="${escapeXml(alpha)}"`);

		rendered.push(`<path ${attributes.join(' ')}/>`);
	}

	if (rendered.length === 0) return '';

	const transform = groupTransform(vector);
	return transform === null
		? rendered.join('')
		: `<g transform="${transform}">${rendered.join('')}</g>`;
}

export function vectorToSvg(
	vector: string,
	colors: Map<string, string>,
	fallbackFill = '#000000'
): string | null {
	if (vector.length > MAX_SOURCE_BYTES) return null;

	const header = vector.match(VECTOR_TAG)?.[0];
	if (header === undefined) return null;

	const width = numeric(header, 'viewportWidth', CANVAS);
	const height = numeric(header, 'viewportHeight', CANVAS);
	if (width <= 0 || height <= 0) return null;

	const body = convertPaths(vector, colors, fallbackFill);
	if (body === '') return null;

	return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${body}</svg>`;
}

export function composeAdaptiveSvg(input: {
	background: { kind: 'color' | 'vector'; value: string } | null;
	foreground: string | null;
	colors: Map<string, string>;
}): string | null {
	const foreground = input.foreground === null ? null : vectorToSvg(input.foreground, input.colors);
	if (foreground === null) return null;

	const inner = foreground.replace(/^<svg[^>]*>/, '').replace(/<\/svg>$/, '');
	const viewBox = foreground.match(/viewBox="([^"]+)"/)?.[1] ?? `0 0 ${CANVAS} ${CANVAS}`;
	const [, , rawWidth, rawHeight] = viewBox.split(/\s+/).map((part) => Number.parseFloat(part));
	const width = rawWidth ?? CANVAS;
	const height = rawHeight ?? CANVAS;

	const layers: string[] = [];

	if (input.background !== null) {
		if (input.background.kind === 'color') {
			const color = resolveColor(input.background.value, input.colors);
			if (color !== null)
				layers.push(`<rect width="${width}" height="${height}" fill="${color}"/>`);
		} else {
			const backgroundSvg = vectorToSvg(input.background.value, input.colors);
			if (backgroundSvg !== null) {
				layers.push(backgroundSvg.replace(/^<svg[^>]*>/, '').replace(/<\/svg>$/, ''));
			}
		}
	}

	layers.push(inner);

	const inset = (VIEWPORT_INSET / CANVAS) * Math.min(width, height);
	const clip = `<clipPath id="c"><rect x="${inset}" y="${inset}" width="${width - inset * 2}" height="${height - inset * 2}" rx="${(width - inset * 2) / 4}"/></clipPath>`;

	return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${clip}<g clip-path="url(#c)">${layers.join('')}</g></svg>`;
}

export function toDataUri(svg: string): string {
	return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}
