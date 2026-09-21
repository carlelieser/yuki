import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { decodeRaster, isWebp } from './raster.ts';
import { encodePng } from './png.ts';

const WEBP_LOSSY_WITH_ALPHA = readFileSync(
	new URL('./__fixtures__/adaptive-foreground.webp', import.meta.url)
);

describe('isWebp', () => {
	it('recognises a riff webp container', () => {
		expect(isWebp(WEBP_LOSSY_WITH_ALPHA)).toBe(true);
	});

	it('does not mistake a png for a webp', () => {
		const png = encodePng({ width: 1, height: 1, pixels: Buffer.from([1, 2, 3, 4]) });

		expect(isWebp(png)).toBe(false);
	});

	it('does not read past the end of a short buffer', () => {
		expect(isWebp(Buffer.from('RIFF'))).toBe(false);
	});
});

describe('decodeRaster', () => {
	it('decodes a png', async () => {
		const png = encodePng({ width: 1, height: 1, pixels: Buffer.from([9, 8, 7, 255]) });

		await expect(decodeRaster(png)).resolves.toMatchObject({ width: 1, height: 1 });
	});

	it('decodes a lossy webp that carries an alpha chunk', async () => {
		const decoded = await decodeRaster(WEBP_LOSSY_WITH_ALPHA);

		expect(decoded).not.toBeNull();
		expect(decoded?.width).toBe(432);
		expect(decoded?.height).toBe(432);
		expect(decoded?.pixels.length).toBe(432 * 432 * 4);
	});

	it('returns null for bytes that are neither', async () => {
		await expect(decodeRaster(Buffer.from('nonsense'))).resolves.toBeNull();
	});
});
