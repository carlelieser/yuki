import { createHash } from 'node:crypto';
import type { IconAsset } from './asset.ts';

export type IconBucket = {
	has: (key: string) => Promise<boolean>;
	put: (key: string, asset: IconAsset) => Promise<void>;
};

export type IconStore = {
	publish: (asset: IconAsset) => Promise<string>;
};

const EXTENSIONS: Record<string, string> = {
	'image/png': 'png',
	'image/webp': 'webp',
	'image/jpeg': 'jpg',
	'image/gif': 'gif',
	'image/svg+xml': 'svg'
};

export function createIconStore(bucket: IconBucket, assetsBaseUrl: string): IconStore {
	const base = assetsBaseUrl.replace(/\/+$/, '');

	return {
		publish: async (asset) => {
			const extension = EXTENSIONS[asset.contentType];
			if (extension === undefined) {
				throw new Error(`publishing icon failed: unsupported content type "${asset.contentType}"`);
			}

			const hash = createHash('sha256').update(asset.bytes).digest('hex');
			const key = `icons/${hash}.${extension}`;
			if (!(await bucket.has(key))) await bucket.put(key, asset);

			return `${base}/${key}`;
		}
	};
}
