import { createServer, type Server } from 'node:http';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildManifest, buildZip } from './archive-fixture.ts';
import { httpApkSource } from './http-source.ts';
import { readApkPackageName } from './package-name.ts';

const ARCHIVE = buildZip([
	{ name: 'AndroidManifest.xml', body: buildManifest('com.termux'), deflate: true }
]);

let server: Server;
let origin: string;

beforeAll(async () => {
	server = createServer((request, response) => {
		const range = request.headers.range;

		if (request.method === 'HEAD') {
			response.writeHead(200, {
				'content-length': String(ARCHIVE.length),
				'accept-ranges': 'bytes'
			});
			response.end();
			return;
		}

		if (range === undefined) {
			response.writeHead(200, { 'content-length': String(ARCHIVE.length) });
			response.end(ARCHIVE);
			return;
		}

		const match = /^bytes=(\d*)-(\d*)$/.exec(range);
		if (match === null || match[1] === '') {
			response.writeHead(501);
			response.end();
			return;
		}

		const start = Number(match[1]);
		const end = match[2] === '' ? ARCHIVE.length - 1 : Number(match[2]);
		const slice = ARCHIVE.subarray(start, end + 1);

		response.writeHead(206, {
			'content-range': `bytes ${start}-${end}/${ARCHIVE.length}`,
			'content-length': String(slice.length)
		});
		response.end(slice);
	});

	await new Promise<void>((resolve) => server.listen(0, resolve));

	const address = server.address();
	if (address === null || typeof address === 'string') throw new Error('no port');
	origin = `http://127.0.0.1:${address.port}`;
});

afterAll(async () => {
	await new Promise<void>((resolve) => server.close(() => resolve()));
});

describe('httpApkSource', () => {
	it('reads a package name from a host that rejects suffix ranges', async () => {
		expect(await readApkPackageName(httpApkSource(`${origin}/app.apk`))).toBe('com.termux');
	});

	it('takes the size from a head request rather than a suffix range', async () => {
		const source = httpApkSource(`${origin}/app.apk`);

		expect(await source.size()).toBe(ARCHIVE.length);
		await expect(source.read(-1000, -1)).rejects.toThrow();
	});
});
