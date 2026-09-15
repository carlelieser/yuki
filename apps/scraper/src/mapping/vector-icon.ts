import { gradientToSvg, parseGradient, type VectorGradient } from './vector-gradient.ts';

const ADAPTIVE_LAYER =
	/<(background|foreground)\b[^>]*android:drawable="@(android:)?(color|drawable|mipmap)\/([^"]+)"/g;
const VECTOR_TAG = /<vector\b[^>]*>/;
const PATH_TAG = /<path\b[^>]*?(?:\/>|>([\s\S]*?)<\/path>)/g;
const GROUP_TAG = /<group\b[^>]*>/;
const COLOR_ENTRY = /<color\s+name="([^"]+)"\s*>\s*([^<\s]+)\s*<\/color>/g;

const MAX_SOURCE_BYTES = 64 * 1024;
const MAX_PATHS = 64;
const CANVAS = 108;
const VIEWPORT_INSET = 18;
const CORNER_RADIUS = 0.2;
const MAX_COLOR_HOPS = 8;

const FRAMEWORK_COLORS = new Map([
	['white', '#FFFFFFFF'],
	['black', '#FF000000'],
	['transparent', '#00000000'],
	['background_light', '#FFFFFFFF'],
	['background_dark', '#FF000000'],
	['primary_text_light', '#FF000000'],
	['primary_text_dark', '#FFFFFFFF'],
	['secondary_text_light', '#FF666666'],
	['secondary_text_dark', '#FFBEBEBE'],
	['holo_blue_light', '#FF33B5E5'],
	['holo_blue_dark', '#FF0099CC'],
	['holo_blue_bright', '#FF00DDFF'],
	['holo_green_light', '#FF99CC00'],
	['holo_green_dark', '#FF669900'],
	['holo_red_light', '#FFFF4444'],
	['holo_red_dark', '#FFCC0000'],
	['holo_orange_light', '#FFFFBB33'],
	['holo_orange_dark', '#FFFF8800'],
	['holo_purple', '#FFAA66CC'],
	['darker_gray', '#FFAAAAAA'],
	['background_holo_dark', '#FF000000'],
	['background_holo_light', '#FFFFFFFF']
]);

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
		if (name !== undefined && value !== undefined) {
			if (value.startsWith('#') || value.startsWith('@color/') || value.startsWith('@android:color/')) {
				colors.set(name, value);
			}
		}
	}

	return colors;
}

export function parseAdaptiveIcon(xml: string): AdaptiveIconRefs {
	const refs: AdaptiveIconRefs = { background: null, foreground: null };

	for (const match of xml.matchAll(ADAPTIVE_LAYER)) {
		const layer = match[1];
		const framework = match[2] !== undefined;
		const kind = match[3];
		const name = match[4];
		if (name === undefined || kind === 'mipmap') continue;

		const ref = {
			kind: kind === 'color' ? ('color' as const) : ('drawable' as const),
			name: kind === 'color' && framework ? `android:${name}` : name
		};
		if (layer === 'background') refs.background = ref;
		if (layer === 'foreground') refs.foreground = ref;
	}

	return refs;
}

function resolveColor(raw: string | null, colors: Map<string, string>): string | null {
	if (raw === null) return null;

	let value: string | null = raw;
	const seen = new Set<string>();

	for (let hop = 0; value !== null && value.startsWith('@'); hop += 1) {
		if (hop >= MAX_COLOR_HOPS || seen.has(value)) return null;
		seen.add(value);

		const reference: string = value;
		const isFramework = reference.startsWith('@android:color/');
		const key = reference.replace(/^@(android:)?color\//, '');

		value = isFramework ? (FRAMEWORK_COLORS.get(key) ?? null) : (colors.get(key) ?? null);
	}

	if (value === null || !/^#(?:[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})$/i.test(value)) return null;

	if (value.length === 9) {
		const alpha = value.slice(1, 3);
		const rgb = value.slice(3);
		return `#${rgb}${alpha}`;
	}

	return value;
}

function resolveColorGradient(
	raw: string | null,
	colors: Map<string, string>,
	gradients: Map<string, string>
): VectorGradient | null {
	if (raw === null || !raw.startsWith('@color/')) return null;

	const source = gradients.get(raw.slice('@color/'.length));
	if (source === undefined) return null;

	return parseGradient(source, (value) => resolveColor(value, colors));
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
	const pivotX = numeric(group, 'pivotX', 0);
	const pivotY = numeric(group, 'pivotY', 0);

	if (translateX !== 0 || translateY !== 0) parts.push(`translate(${translateX} ${translateY})`);

	const pivoted = pivotX !== 0 || pivotY !== 0;
	if (pivoted) parts.push(`translate(${pivotX} ${pivotY})`);
	if (rotation !== 0) parts.push(`rotate(${rotation})`);
	if (scaleX !== 1 || scaleY !== 1) parts.push(`scale(${scaleX} ${scaleY})`);
	if (pivoted) parts.push(`translate(${-pivotX} ${-pivotY})`);

	return parts.length === 0 ? null : parts.join(' ');
}

function convertPaths(
	vector: string,
	colors: Map<string, string>,
	fallbackFill: string,
	idPrefix: string,
	gradients: Map<string, string>
): string {
	const rendered: string[] = [];
	const definitions: string[] = [];

	for (const match of vector.matchAll(PATH_TAG)) {
		if (rendered.length >= MAX_PATHS) break;

		const tag = match[0];
		const data = attribute(tag, 'pathData');
		if (data === null || data.trim() === '') continue;

		const gradient =
			parseGradient(match[1] ?? '', (raw) => resolveColor(raw, colors)) ??
			resolveColorGradient(attribute(tag, 'fillColor'), colors, gradients);
		const fill = resolveColor(attribute(tag, 'fillColor'), colors);
		const stroke = resolveColor(attribute(tag, 'strokeColor'), colors);
		const strokeWidth = numeric(tag, 'strokeWidth', 0);

		const declaresFill = attribute(tag, 'fillColor') !== null;
		const declaresStroke = attribute(tag, 'strokeColor') !== null;
		if (gradient === null && !declaresFill && !declaresStroke) continue;

		const attributes = [`d="${escapeXml(data.trim())}"`];

		if (gradient !== null) {
			const id = `${idPrefix}${definitions.length}`;
			const definition = gradientToSvg(gradient, id);

			if (definition === '') {
				attributes.push(`fill="${gradient.stops[0]?.color ?? fallbackFill}"`);
			} else {
				definitions.push(definition);
				attributes.push(`fill="url(#${id})"`);
			}
		} else {
			attributes.push(`fill="${fill ?? (stroke === null ? fallbackFill : 'none')}"`);
		}

		if (stroke !== null && strokeWidth > 0) {
			attributes.push(`stroke="${stroke}"`, `stroke-width="${strokeWidth}"`);

			const cap = attribute(tag, 'strokeLineCap');
			const join = attribute(tag, 'strokeLineJoin');
			if (cap !== null) attributes.push(`stroke-linecap="${escapeXml(cap)}"`);
			if (join !== null) attributes.push(`stroke-linejoin="${escapeXml(join)}"`);
		}

		const alpha = attribute(tag, 'fillAlpha');
		if (alpha !== null) attributes.push(`fill-opacity="${escapeXml(alpha)}"`);

		if (attribute(tag, 'fillType')?.toLowerCase() === 'evenodd') {
			attributes.push('fill-rule="evenodd"');
		}

		rendered.push(`<path ${attributes.join(' ')}/>`);
	}

	if (rendered.length === 0) return '';

	const defs = definitions.length === 0 ? '' : `<defs>${definitions.join('')}</defs>`;
	const transform = groupTransform(vector);
	return transform === null
		? `${defs}${rendered.join('')}`
		: `${defs}<g transform="${transform}">${rendered.join('')}</g>`;
}

export function vectorToSvg(
	vector: string,
	colors: Map<string, string>,
	options: { fallbackFill?: string; idPrefix?: string; gradients?: Map<string, string> } = {}
): string | null {
	const fallbackFill = options.fallbackFill ?? '#000000';
	const idPrefix = options.idPrefix ?? 'g';
	const gradients = options.gradients ?? new Map<string, string>();

	if (vector.length > MAX_SOURCE_BYTES) return null;

	const header = vector.match(VECTOR_TAG)?.[0];
	if (header === undefined) return null;

	const width = numeric(header, 'viewportWidth', CANVAS);
	const height = numeric(header, 'viewportHeight', CANVAS);
	if (width <= 0 || height <= 0) return null;

	const body = convertPaths(vector, colors, fallbackFill, idPrefix, gradients);
	if (body === '') return null;

	return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${body}</svg>`;
}

function normaliseLayer(svg: string): string {
	const inner = svg.replace(/^<svg[^>]*>/, '').replace(/<\/svg>$/, '');
	const viewBox = svg.match(/viewBox="([^"]+)"/)?.[1];
	if (viewBox === undefined) return inner;

	const [, , rawWidth, rawHeight] = viewBox.split(/\s+/).map((part) => Number.parseFloat(part));
	const width = rawWidth !== undefined && rawWidth > 0 ? rawWidth : CANVAS;
	const height = rawHeight !== undefined && rawHeight > 0 ? rawHeight : CANVAS;

	const scaleX = CANVAS / width;
	const scaleY = CANVAS / height;
	if (scaleX === 1 && scaleY === 1) return inner;

	return `<g transform="scale(${scaleX} ${scaleY})">${inner}</g>`;
}

export function composeAdaptiveSvg(input: {
	background: { kind: 'color' | 'vector'; value: string } | null;
	foreground: string | null;
	colors: Map<string, string>;
	gradients?: Map<string, string>;
}): string | null {
	const gradients = input.gradients ?? new Map<string, string>();
	const foreground =
		input.foreground === null
			? null
			: vectorToSvg(input.foreground, input.colors, { idPrefix: 'fg', gradients });
	if (foreground === null) return null;

	const width = CANVAS;
	const height = CANVAS;
	const inner = normaliseLayer(foreground);

	const layers: string[] = [];

	if (input.background !== null) {
		if (input.background.kind === 'color') {
			const color = resolveColor(input.background.value, input.colors);
			const gradient = resolveColorGradient(input.background.value, input.colors, gradients);

			if (gradient !== null) {
				const definition = gradientToSvg(gradient, 'bgc');
				if (definition === '') {
					const first = gradient.stops[0]?.color;
					if (first !== undefined)
						layers.push(`<rect width="${width}" height="${height}" fill="${first}"/>`);
				} else {
					layers.push(
						`<defs>${definition}</defs><rect width="${width}" height="${height}" fill="url(#bgc)"/>`
					);
				}
			} else if (color !== null) {
				layers.push(`<rect width="${width}" height="${height}" fill="${color}"/>`);
			}
		} else {
			const backgroundSvg = vectorToSvg(input.background.value, input.colors, {
				idPrefix: 'bg',
				gradients
			});
			if (backgroundSvg !== null) layers.push(normaliseLayer(backgroundSvg));
		}
	}

	layers.push(inner);

	const inset = (VIEWPORT_INSET / CANVAS) * Math.min(width, height);
	const clip = `<clipPath id="c"><rect x="${inset}" y="${inset}" width="${width - inset * 2}" height="${height - inset * 2}" rx="${CORNER_RADIUS * Math.min(width, height)}"/></clipPath>`;

	return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${clip}<g clip-path="url(#c)">${layers.join('')}</g></svg>`;
}

export function toDataUri(svg: string): string {
	return `data:image/svg+xml;base64,${Buffer.from(svg, 'utf8').toString('base64')}`;
}
