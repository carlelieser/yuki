import { encodePng, type RgbaImage } from './png.ts';
import { decodeRaster } from './raster.ts';

const CHANNELS = 4;
const OUTPUT_SIZE = 432;
const VIEWPORT_SCALE = 72 / 108;
const CORNER_RADIUS = 0.2;

function sample(image: RgbaImage, x: number, y: number): [number, number, number, number] {
	const column = Math.round(x);
	const row = Math.round(y);
	if (column < 0 || row < 0 || column >= image.width || row >= image.height) return [0, 0, 0, 0];

	const offset = (row * image.width + column) * CHANNELS;

	return [
		image.pixels[offset] ?? 0,
		image.pixels[offset + 1] ?? 0,
		image.pixels[offset + 2] ?? 0,
		image.pixels[offset + 3] ?? 0
	];
}

function isInsideMask(x: number, y: number, size: number): boolean {
	const radius = size * CORNER_RADIUS;
	const nearLeft = x < radius;
	const nearRight = x > size - radius;
	const nearTop = y < radius;
	const nearBottom = y > size - radius;

	if (!nearLeft && !nearRight) return true;
	if (!nearTop && !nearBottom) return true;

	const centreX = nearLeft ? radius : size - radius;
	const centreY = nearTop ? radius : size - radius;
	const dx = x - centreX;
	const dy = y - centreY;

	return dx * dx + dy * dy <= radius * radius;
}

function blend(under: [number, number, number, number], over: [number, number, number, number]) {
	const opacity = over[3] / 255;
	if (opacity === 0) return under;
	if (opacity === 1) return over;

	const keep = 1 - opacity;
	return [
		Math.round(over[0] * opacity + under[0] * keep),
		Math.round(over[1] * opacity + under[1] * keep),
		Math.round(over[2] * opacity + under[2] * keep),
		Math.round(over[3] + under[3] * keep)
	] as [number, number, number, number];
}

function layerAt(layer: RgbaImage | null, x: number, y: number, size: number) {
	if (layer === null) return [0, 0, 0, 0] as [number, number, number, number];

	const centred = (value: number) => (value / size - 0.5) * VIEWPORT_SCALE + 0.5;
	return sample(layer, centred(x) * layer.width, centred(y) * layer.height);
}

export async function composeAdaptiveRaster(
	background: Buffer,
	foreground: Buffer
): Promise<Buffer | null> {
	const under = await decodeRaster(background);
	const over = await decodeRaster(foreground);
	if (under === null && over === null) return null;

	const size = OUTPUT_SIZE;
	const pixels = Buffer.alloc(size * size * CHANNELS);

	for (let y = 0; y < size; y += 1) {
		for (let x = 0; x < size; x += 1) {
			const target = (y * size + x) * CHANNELS;
			if (!isInsideMask(x, y, size)) continue;

			const merged = blend(layerAt(under, x, y, size), layerAt(over, x, y, size));
			pixels[target] = merged[0];
			pixels[target + 1] = merged[1];
			pixels[target + 2] = merged[2];
			pixels[target + 3] = merged[3];
		}
	}

	return encodePng({ width: size, height: size, pixels });
}

export function toPngDataUri(png: Buffer): string {
	return `data:image/png;base64,${png.toString('base64')}`;
}
