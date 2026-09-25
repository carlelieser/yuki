import { encodePng } from './png.ts';
import { resolveColor } from './vector-icon.ts';

const CHANNELS = 4;
const FULL_CANVAS = /^M0,0[hH]([\d.]+)[vV]([\d.]+)[hH]-?([\d.]+)[zZ]$/;
const PATH_TAG = /<path\b[^>]*>/g;
const SHAPE_TAG = /<shape\b/;
const SOLID_TAG = /<solid\b[^>]*>/;
const SIZE = 432;

function attribute(tag: string, name: string): string | null {
	return tag.match(new RegExp(`android:${name}="([^"]*)"`))?.[1] ?? null;
}

export function readSolidFill(xml: string, colors: Map<string, string>): string | null {
	const paths = [...xml.matchAll(PATH_TAG)].map((match) => match[0]);
	if (paths.length !== 1) return null;

	const tag = paths[0];
	if (tag === undefined) return null;

	const data = attribute(tag, 'pathData')?.replace(/\s+/g, '');
	if (data === undefined || data === null || !FULL_CANVAS.test(data)) return null;

	return resolveColor(attribute(tag, 'fillColor'), colors);
}

export function readShapeFill(xml: string): string | null {
	if (!SHAPE_TAG.test(xml)) return null;

	const solid = xml.match(SOLID_TAG)?.[0];
	if (solid === undefined) return null;

	return attribute(solid, 'color');
}

export function solidLayer(color: string): Buffer {
	const value = color.replace('#', '');
	const hasAlpha = value.length === 8;

	const red = Number.parseInt(value.slice(0, 2), 16);
	const green = Number.parseInt(value.slice(2, 4), 16);
	const blue = Number.parseInt(value.slice(4, 6), 16);
	const alpha = hasAlpha ? Number.parseInt(value.slice(6, 8), 16) : 0xff;

	const pixels = Buffer.alloc(SIZE * SIZE * CHANNELS);
	for (let offset = 0; offset < pixels.length; offset += CHANNELS) {
		pixels[offset] = red;
		pixels[offset + 1] = green;
		pixels[offset + 2] = blue;
		pixels[offset + 3] = alpha;
	}

	return encodePng({ width: SIZE, height: SIZE, pixels });
}
