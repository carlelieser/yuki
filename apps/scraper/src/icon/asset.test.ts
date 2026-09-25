import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { readIconAsset } from './asset.ts';

const svg = '<svg xmlns="http://www.w3.org/2000/svg"/>';
const png = Buffer.from([137, 80, 78, 71]);

let server: Server;
let baseUrl: string;

beforeAll(async () => {
	server = createServer((request, response) => {
		if (request.url === '/res/icon.png') {
			response.writeHead(200, { 'content-type': 'application/octet-stream' }).end(png);
			return;
		}
		if (request.url === '/avatar') {
			response.writeHead(200, { 'content-type': 'image/webp' }).end(png);
			return;
		}
		if (request.url === '/page') {
			response.writeHead(200, { 'content-type': 'text/html' }).end('<html/>');
			return;
		}
		response.writeHead(404).end();
	});
	await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
	baseUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
});

afterAll(async () => {
	await new Promise<void>((resolve) => server.close(() => resolve()));
});

describe('readIconAsset', () => {
	it('decodes a base64 svg data uri', async () => {
		const asset = await readIconAsset(
			`data:image/svg+xml;base64,${Buffer.from(svg).toString('base64')}`
		);

		expect(asset.contentType).toBe('image/svg+xml');
		expect(Buffer.from(asset.bytes).toString()).toBe(svg);
	});

	it('decodes a percent-encoded data uri', async () => {
		const asset = await readIconAsset(`data:image/svg+xml,${encodeURIComponent(svg)}`);

		expect(Buffer.from(asset.bytes).toString()).toBe(svg);
	});

	it('types a downloaded icon by its file extension', async () => {
		const asset = await readIconAsset(`${baseUrl}/res/icon.png`);

		expect(asset.contentType).toBe('image/png');
		expect(Buffer.from(asset.bytes)).toEqual(png);
	});

	it('falls back to the image content type when the url has no extension', async () => {
		const asset = await readIconAsset(`${baseUrl}/avatar`);

		expect(asset.contentType).toBe('image/webp');
	});

	it('rejects a download that is not an image', async () => {
		await expect(readIconAsset(`${baseUrl}/page`)).rejects.toThrow('unexpected content type');
	});

	it('names the url when the download fails', async () => {
		await expect(readIconAsset(`${baseUrl}/missing.png`)).rejects.toThrow(
			`downloading icon ${baseUrl}/missing.png failed with status 404`
		);
	});
});
