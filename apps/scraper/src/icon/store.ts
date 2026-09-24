import { createHash } from 'node:crypto';

export type IconBucket = {
	has: (key: string) => Promise<boolean>;
	put: (key: string, png: Uint8Array) => Promise<void>;
};

export type IconStore = {
	publish: (png: Uint8Array) => Promise<string>;
};

export function createIconStore(bucket: IconBucket, assetsBaseUrl: string): IconStore {
	const base = assetsBaseUrl.replace(/\/+$/, '');

	return {
		publish: async (png) => {
			const key = `icons/${createHash('sha256').update(png).digest('hex')}.png`;
			if (!(await bucket.has(key))) await bucket.put(key, png);

			return `${base}/${key}`;
		}
	};
}
