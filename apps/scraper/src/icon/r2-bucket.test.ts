import { createServer, type IncomingMessage, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { createR2Bucket, type R2Bucket } from './r2-bucket.ts';

const pages: Record<string, string> = {
	'': `<ListBucketResult><IsTruncated>true</IsTruncated><NextContinuationToken>next&amp;1</NextContinuationToken>
		<Contents><Key>icons/a.png</Key><LastModified>2026-09-01T00:00:00.000Z</LastModified></Contents>
		<Contents><Key>icons/b&amp;c.svg</Key><LastModified>2026-09-02T00:00:00.000Z</LastModified></Contents>
	</ListBucketResult>`,
	'next&1': `<ListBucketResult><IsTruncated>false</IsTruncated>
		<Contents><Key>icons/d.png</Key><LastModified>2026-09-03T00:00:00.000Z</LastModified></Contents>
	</ListBucketResult>`
};

let server: Server;
let bucket: R2Bucket;
let requests: { method: string; url: string }[];

function handle(request: IncomingMessage): { status: number; body?: string } {
	const url = new URL(request.url ?? '/', 'http://localhost');
	requests.push({ method: request.method ?? '', url: url.pathname + url.search });

	if (request.method === 'GET' && url.pathname === '/yuki-assets') {
		return { status: 200, body: pages[url.searchParams.get('continuation-token') ?? ''] };
	}
	if (request.method === 'DELETE' && url.pathname === '/yuki-assets/icons/broken.png') {
		return { status: 403 };
	}
	return { status: request.method === 'DELETE' ? 204 : 404 };
}

beforeAll(async () => {
	server = createServer((request, response) => {
		const { status, body } = handle(request);
		response.writeHead(status).end(body);
	});
	await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
	bucket = createR2Bucket({
		endpoint: `http://127.0.0.1:${(server.address() as AddressInfo).port}`,
		accessKeyId: 'key',
		secretAccessKey: 'secret',
		bucket: 'yuki-assets'
	});
});

beforeEach(() => {
	requests = [];
});

afterAll(async () => {
	await new Promise<void>((resolve) => server.close(() => resolve()));
});

describe('R2 bucket listing', () => {
	it('follows continuation tokens and decodes keys', async () => {
		const objects = await bucket.list('icons/');

		expect(objects.map((object) => object.key)).toEqual([
			'icons/a.png',
			'icons/b&c.svg',
			'icons/d.png'
		]);
		expect(objects[0]?.lastModified.toISOString()).toBe('2026-09-01T00:00:00.000Z');
		expect(
			requests.map((request) => new URL(request.url, 'http://x').searchParams.get('prefix'))
		).toEqual(['icons/', 'icons/']);
	});
});

describe('R2 bucket removal', () => {
	it('deletes the object by key', async () => {
		await bucket.remove('icons/a.png');

		expect(requests).toEqual([{ method: 'DELETE', url: '/yuki-assets/icons/a.png' }]);
	});

	it('names the key when a delete fails', async () => {
		await expect(bucket.remove('icons/broken.png')).rejects.toThrow(
			'deleting icons/broken.png from R2 failed with status 403'
		);
	});
});
