import { createHash } from 'node:crypto';
import { describe, expect, it } from 'vitest';
import type { IconAsset } from './asset.ts';
import { createIconStore, type IconBucket } from './store.ts';

function memoryBucket(existing: string[] = []) {
	const objects = new Map<string, IconAsset>(
		existing.map((key) => [key, { bytes: new Uint8Array(), contentType: 'image/png' }])
	);
	const puts: string[] = [];
	const bucket: IconBucket = {
		has: async (key) => objects.has(key),
		put: async (key, asset) => {
			puts.push(key);
			objects.set(key, asset);
		}
	};
	return { bucket, objects, puts };
}

const png: IconAsset = {
	bytes: new Uint8Array([137, 80, 78, 71, 1, 2, 3]),
	contentType: 'image/png'
};
const hash = createHash('sha256').update(png.bytes).digest('hex');

describe('createIconStore', () => {
	it('uploads the icon under a content hash and returns its public url', async () => {
		const { bucket, objects } = memoryBucket();

		const url = await createIconStore(bucket, 'https://icons.example.com').publish(png);

		expect(url).toBe(`https://icons.example.com/icons/${hash}.png`);
		expect(objects.get(`icons/${hash}.png`)).toEqual(png);
	});

	it('names svg icons with an svg extension', async () => {
		const { bucket } = memoryBucket();
		const svg: IconAsset = { bytes: png.bytes, contentType: 'image/svg+xml' };

		const url = await createIconStore(bucket, 'https://icons.example.com').publish(svg);

		expect(url).toBe(`https://icons.example.com/icons/${hash}.svg`);
	});

	it('skips the upload when the icon is already stored', async () => {
		const { bucket, puts } = memoryBucket([`icons/${hash}.png`]);

		await createIconStore(bucket, 'https://icons.example.com').publish(png);

		expect(puts).toEqual([]);
	});

	it('ignores a trailing slash on the assets base url', async () => {
		const { bucket } = memoryBucket();

		const url = await createIconStore(bucket, 'https://icons.example.com/').publish(png);

		expect(url).toBe(`https://icons.example.com/icons/${hash}.png`);
	});

	it('refuses content that is not an image', async () => {
		const { bucket } = memoryBucket();
		const text: IconAsset = { bytes: png.bytes, contentType: 'text/html' };

		await expect(
			createIconStore(bucket, 'https://icons.example.com').publish(text)
		).rejects.toThrow('unsupported content type "text/html"');
	});
});
