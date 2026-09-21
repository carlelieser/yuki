import { describe, expect, it } from 'vitest';
import { composeAdaptiveRaster } from './adaptive-raster.ts';
import { decodePng, encodePng, type RgbaImage } from './png.ts';

function solid(size: number, colour: [number, number, number, number]): RgbaImage {
	const pixels = Buffer.alloc(size * size * 4);

	for (let index = 0; index < size * size; index += 1) {
		pixels[index * 4] = colour[0];
		pixels[index * 4 + 1] = colour[1];
		pixels[index * 4 + 2] = colour[2];
		pixels[index * 4 + 3] = colour[3];
	}

	return { width: size, height: size, pixels };
}

function centredSquare(size: number, colour: [number, number, number, number]): RgbaImage {
	const image = solid(size, [0, 0, 0, 0]);
	const from = Math.floor(size / 4);
	const to = size - from;

	for (let y = from; y < to; y += 1) {
		for (let x = from; x < to; x += 1) {
			const offset = (y * size + x) * 4;
			image.pixels[offset] = colour[0];
			image.pixels[offset + 1] = colour[1];
			image.pixels[offset + 2] = colour[2];
			image.pixels[offset + 3] = colour[3];
		}
	}

	return image;
}

function pixelAt(png: Buffer, x: number, y: number): [number, number, number, number] {
	const image = decodePng(png);
	if (image === null) throw new Error('composed output was not a decodable png');

	const offset = (y * image.width + x) * 4;
	return [
		image.pixels[offset] ?? 0,
		image.pixels[offset + 1] ?? 0,
		image.pixels[offset + 2] ?? 0,
		image.pixels[offset + 3] ?? 0
	];
}

describe('composeAdaptiveRaster', () => {
	const background = encodePng(solid(108, [255, 0, 0, 255]));
	const foreground = encodePng(centredSquare(108, [0, 0, 255, 255]));

	it('keeps the foreground artwork rather than serving a layer on its own', async () => {
		const composed = await composeAdaptiveRaster(background, foreground);
		if (composed === null) throw new Error('expected a composed icon');

		const image = decodePng(composed);
		const centre = pixelAt(
			composed,
			Math.floor((image?.width ?? 0) / 2),
			Math.floor((image?.height ?? 0) / 2)
		);

		expect(centre[2]).toBeGreaterThan(centre[0]);
		expect(centre[3]).toBe(255);
	});

	it('paints the background where the foreground is transparent', async () => {
		const composed = await composeAdaptiveRaster(background, foreground);
		if (composed === null) throw new Error('expected a composed icon');

		const image = decodePng(composed);
		const size = image?.width ?? 0;
		const nearEdge = pixelAt(composed, Math.floor(size / 2), Math.floor(size * 0.06));

		expect(nearEdge[0]).toBeGreaterThan(nearEdge[2]);
		expect(nearEdge[3]).toBe(255);
	});

	it('crops into the safe zone rather than zooming away from it', async () => {
		const composed = await composeAdaptiveRaster(background, foreground);
		if (composed === null) throw new Error('expected a composed icon');

		const image = decodePng(composed);
		const size = image?.width ?? 0;

		const inset = pixelAt(composed, Math.floor(size / 2), Math.floor(size * 0.2));
		expect(inset[2]).toBeGreaterThan(inset[0]);
	});

	it('clears the corners so the icon reads as a rounded tile', async () => {
		const composed = await composeAdaptiveRaster(background, foreground);
		if (composed === null) throw new Error('expected a composed icon');

		expect(pixelAt(composed, 0, 0)[3]).toBe(0);
	});

	it('returns null when neither layer decodes', async () => {
		await expect(
			composeAdaptiveRaster(Buffer.from('not a png'), Buffer.from('also not'))
		).resolves.toBeNull();
	});
});

describe('png round trip', () => {
	it('decodes what it encodes', async () => {
		const source = solid(4, [10, 20, 30, 255]);
		const decoded = decodePng(encodePng(source));

		expect(decoded?.width).toBe(4);
		expect(decoded?.pixels.subarray(0, 4)).toEqual(Buffer.from([10, 20, 30, 255]));
	});

	it('rejects input that is not a png', async () => {
		expect(decodePng(Buffer.from('nope'))).toBeNull();
	});
});
