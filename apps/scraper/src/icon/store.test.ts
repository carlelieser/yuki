import { createHash } from 'node:crypto';
import { describe, expect, it } from 'vitest';
import { createIconStore, type IconBucket } from './store.ts';

function memoryBucket(existing: string[] = []) {
	const objects = new Map<string, Uint8Array>(existing.map((key) => [key, new Uint8Array()]));
	const puts: string[] = [];
	const bucket: IconBucket = {
		has: async (key) => objects.has(key),
		put: async (key, png) => {
			puts.push(key);
			objects.set(key, png);
		}
	};
	return { bucket, objects, puts };
}

const png = new Uint8Array([137, 80, 78, 71, 1, 2, 3]);
const key = `icons/${createHash('sha256').update(png).digest('hex')}.png`;

describe('createIconStore', () => {
	it('uploads the png under a content hash and returns its public url', async () => {
		const { bucket, objects } = memoryBucket();

		const url = await createIconStore(bucket, 'https://icons.example.com').publish(png);

		expect(url).toBe(`https://icons.example.com/${key}`);
		expect(objects.get(key)).toEqual(png);
	});

	it('skips the upload when the icon is already stored', async () => {
		const { bucket, puts } = memoryBucket([key]);

		const url = await createIconStore(bucket, 'https://icons.example.com').publish(png);

		expect(url).toBe(`https://icons.example.com/${key}`);
		expect(puts).toEqual([]);
	});

	it('ignores a trailing slash on the public base url', async () => {
		const { bucket } = memoryBucket();

		const url = await createIconStore(bucket, 'https://icons.example.com/').publish(png);

		expect(url).toBe(`https://icons.example.com/${key}`);
	});
});
