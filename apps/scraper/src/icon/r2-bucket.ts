import { AwsClient } from 'aws4fetch';
import type { IconBucket } from './store.ts';

const REQUEST_TIMEOUT_MILLIS = 30_000;
const CACHE_CONTROL = 'public, max-age=31536000, immutable';

export type R2Config = {
	endpoint: string;
	accessKeyId: string;
	secretAccessKey: string;
	bucket: string;
};

export function createR2Bucket(config: R2Config): IconBucket {
	const client = new AwsClient({
		accessKeyId: config.accessKeyId,
		secretAccessKey: config.secretAccessKey,
		service: 's3',
		region: 'auto'
	});
	const endpoint = `${config.endpoint.replace(/\/+$/, '')}/${config.bucket}`;

	return {
		has: async (key) => {
			const response = await client.fetch(`${endpoint}/${key}`, {
				method: 'HEAD',
				signal: AbortSignal.timeout(REQUEST_TIMEOUT_MILLIS)
			});
			if (response.status === 404) return false;
			if (!response.ok) {
				throw new Error(`checking ${key} in R2 failed with status ${response.status}`);
			}

			return true;
		},
		put: async (key, asset) => {
			const response = await client.fetch(`${endpoint}/${key}`, {
				method: 'PUT',
				body: asset.bytes,
				headers: { 'content-type': asset.contentType, 'cache-control': CACHE_CONTROL },
				signal: AbortSignal.timeout(REQUEST_TIMEOUT_MILLIS)
			});
			if (!response.ok) {
				throw new Error(`uploading ${key} to R2 failed with status ${response.status}`);
			}
		}
	};
}
