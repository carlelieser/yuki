import { convertPaths } from './vector-render.ts';
export { resolveColor, resolveColorGradient } from './vector-colors.ts';
import { numeric } from './vector-attributes.ts';

const ADAPTIVE_LAYER =
	/<(background|foreground)\b[^>]*android:drawable="@(android:)?(color|drawable|mipmap)\/([^"]+)"/g;
const VECTOR_TAG = /<vector\b[^>]*>/;
const COLOR_ENTRY = /<color\s+name="([^"]+)"\s*>\s*([^<\s]+)\s*<\/color>/g;

const MAX_SOURCE_BYTES = 64 * 1024;
export const CANVAS = 108;
export const VIEWPORT_INSET = 18;
export const CORNER_RADIUS = 0.2;

export type AdaptiveIconRefs = {
	background: { kind: 'color' | 'drawable'; name: string } | null;
	foreground: { kind: 'color' | 'drawable'; name: string } | null;
};

export function parseColors(xml: string): Map<string, string> {
	const colors = new Map<string, string>();

	for (const match of xml.matchAll(COLOR_ENTRY)) {
		const name = match[1];
		const value = match[2];
		if (name !== undefined && value !== undefined) {
			if (
				value.startsWith('#') ||
				value.startsWith('@color/') ||
				value.startsWith('@android:color/')
			) {
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

export function vectorToSvg(
	vector: string,
	colors: Map<string, string>,
	options: {
		fallbackFill?: string | null;
		idPrefix?: string;
		gradients?: Map<string, string>;
	} = {}
): string | null {
	const fallbackFill = options.fallbackFill === undefined ? '#000000' : options.fallbackFill;
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
