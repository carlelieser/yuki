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

export type StoredObject = {
	key: string;
	lastModified: Date;
};

export type R2Bucket = IconBucket & {
	list: (prefix: string) => Promise<StoredObject[]>;
	remove: (key: string) => Promise<void>;
};

type ListPage = { objects: StoredObject[]; continuationToken: string | null };

export function createR2Bucket(config: R2Config): R2Bucket {
	const client = new AwsClient({
		accessKeyId: config.accessKeyId,
		secretAccessKey: config.secretAccessKey,
		service: 's3',
		region: 'auto'
	});
	const endpoint = `${config.endpoint.replace(/\/+$/, '')}/${config.bucket}`;

	async function send(url: string, init: RequestInit, operation: string): Promise<Response> {
		const response = await client.fetch(url, {
			...init,
			signal: AbortSignal.timeout(REQUEST_TIMEOUT_MILLIS)
		});
		if (response.status === 404 && init.method === 'HEAD') return response;
		if (!response.ok) throw new Error(`${operation} failed with status ${response.status}`);

		return response;
	}

	async function listPage(prefix: string, token: string | null): Promise<ListPage> {
		const query = new URLSearchParams({ 'list-type': '2', prefix });
		if (token !== null) query.set('continuation-token', token);

		const response = await send(
			`${endpoint}?${query}`,
			{ method: 'GET' },
			`listing ${prefix} in R2`
		);
		return parseListPage(await response.text());
	}

	return {
		has: async (key) => {
			const response = await send(
				`${endpoint}/${key}`,
				{ method: 'HEAD' },
				`checking ${key} in R2`
			);
			return response.status !== 404;
		},
		put: async (key, asset) => {
			await send(
				`${endpoint}/${key}`,
				{
					method: 'PUT',
					body: asset.bytes,
					headers: { 'content-type': asset.contentType, 'cache-control': CACHE_CONTROL }
				},
				`uploading ${key} to R2`
			);
		},
		list: async (prefix) => {
			const objects: StoredObject[] = [];
			let token: string | null = null;

			do {
				const page = await listPage(prefix, token);
				objects.push(...page.objects);
				token = page.continuationToken;
			} while (token !== null);

			return objects;
		},
		remove: async (key) => {
			await send(`${endpoint}/${key}`, { method: 'DELETE' }, `deleting ${key} from R2`);
		}
	};
}

function parseListPage(xml: string): ListPage {
	const objects = [...xml.matchAll(/<Contents>([\s\S]*?)<\/Contents>/g)].map(([, entry = '']) => ({
		key: decodeXml(tagValue(entry, 'Key') ?? ''),
		lastModified: new Date(tagValue(entry, 'LastModified') ?? 0)
	}));
	const isTruncated = tagValue(xml, 'IsTruncated') === 'true';
	const token = tagValue(xml, 'NextContinuationToken');

	return { objects, continuationToken: isTruncated && token !== null ? decodeXml(token) : null };
}

function tagValue(xml: string, tag: string): string | null {
	return new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`).exec(xml)?.[1] ?? null;
}

function decodeXml(value: string): string {
	return value
		.replaceAll('&lt;', '<')
		.replaceAll('&gt;', '>')
		.replaceAll('&quot;', '"')
		.replaceAll('&apos;', "'")
		.replaceAll('&amp;', '&');
}
