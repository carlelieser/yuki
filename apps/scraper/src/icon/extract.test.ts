import { existsSync } from 'node:fs';
import { chmod, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { createApkIconExtractor } from './extract.ts';

const apkBytes = Buffer.from('fake apk contents');

let server: Server;
let baseUrl: string;
let scriptsDir: string;

beforeAll(async () => {
	server = createServer((request, response) => {
		if (request.url === '/app.apk') {
			response.writeHead(200, { 'content-length': apkBytes.length });
			response.end(apkBytes);
			return;
		}
		response.writeHead(404).end();
	});
	await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
	baseUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
	scriptsDir = await mkdtemp(join(tmpdir(), 'yuki-extract-test-'));
});

afterAll(async () => {
	await new Promise<void>((resolve) => server.close(() => resolve()));
	await rm(scriptsDir, { recursive: true, force: true });
});

async function fakeExtractor(name: string, body: string): Promise<string> {
	const path = join(scriptsDir, name);
	await writeFile(path, `#!/bin/sh\n${body}\n`);
	await chmod(path, 0o755);
	return path;
}

describe('createApkIconExtractor', () => {
	it('returns the png the extractor writes for the downloaded apk', async () => {
		const binary = await fakeExtractor('copies', 'cp "$2" "$4/app_ic_launcher.png"');

		const icon = await createApkIconExtractor(binary)(`${baseUrl}/app.apk`);

		expect(icon).toEqual(apkBytes);
	});

	it('returns null when the apk declares no icon', async () => {
		const binary = await fakeExtractor('silent', 'exit 0');

		const icon = await createApkIconExtractor(binary)(`${baseUrl}/app.apk`);

		expect(icon).toBeNull();
	});

	it('removes the downloaded apk once extraction finishes', async () => {
		const record = join(scriptsDir, 'workdir.txt');
		const binary = await fakeExtractor('records', `echo "$4" > "${record}"`);

		await createApkIconExtractor(binary)(`${baseUrl}/app.apk`);

		const workDir = (await readFile(record, 'utf8')).trim();
		expect(existsSync(workDir)).toBe(false);
	});

	it('names the url when the download fails', async () => {
		const binary = await fakeExtractor('unused', 'exit 0');

		await expect(createApkIconExtractor(binary)(`${baseUrl}/missing.apk`)).rejects.toThrow(
			`downloading ${baseUrl}/missing.apk failed with status 404`
		);
	});

	it('fails when the extractor exits with an error', async () => {
		const binary = await fakeExtractor('broken', 'echo "corrupt apk" >&2; exit 1');

		await expect(createApkIconExtractor(binary)(`${baseUrl}/app.apk`)).rejects.toThrow(
			/extracting the icon from .* failed/
		);
	});
});
