import { execFile } from 'node:child_process';
import { createWriteStream } from 'node:fs';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { pipeline } from 'node:stream/promises';
import { promisify } from 'node:util';

const runFile = promisify(execFile);

const DOWNLOAD_TIMEOUT_MILLIS = 180_000;
const EXTRACT_TIMEOUT_MILLIS = 60_000;
const MAX_APK_BYTES = 300 * 1024 * 1024;
const APK_FILE = 'app.apk';
const ICON_FILE = 'app_ic_launcher.png';

export type ApkIconExtractor = (downloadUrl: string) => Promise<Buffer | null>;

export function createApkIconExtractor(binary: string): ApkIconExtractor {
	return async (downloadUrl) => {
		const workDir = await mkdtemp(join(tmpdir(), 'yuki-apk-'));

		try {
			const apkPath = join(workDir, APK_FILE);
			await download(downloadUrl, apkPath);
			await extract(binary, apkPath, workDir);
			return await readIcon(join(workDir, ICON_FILE));
		} finally {
			await rm(workDir, { recursive: true, force: true });
		}
	};
}

async function download(url: string, destination: string): Promise<void> {
	const response = await fetch(url, { signal: AbortSignal.timeout(DOWNLOAD_TIMEOUT_MILLIS) });
	if (!response.ok || response.body === null) {
		throw new Error(`downloading ${url} failed with status ${response.status}`);
	}

	const declaredSize = Number(response.headers.get('content-length'));
	if (declaredSize > MAX_APK_BYTES) {
		throw new Error(`downloading ${url} failed: ${declaredSize} bytes exceeds the size limit`);
	}

	await pipeline(capped(response.body, url), createWriteStream(destination));
}

async function* capped(body: AsyncIterable<Uint8Array>, url: string): AsyncIterable<Uint8Array> {
	let received = 0;

	for await (const chunk of body) {
		received += chunk.byteLength;
		if (received > MAX_APK_BYTES) {
			throw new Error(`downloading ${url} failed: body exceeds the size limit`);
		}
		yield chunk;
	}
}

async function extract(binary: string, apkPath: string, outDir: string): Promise<void> {
	try {
		await runFile(binary, ['-apk', apkPath, '-o', outDir], { timeout: EXTRACT_TIMEOUT_MILLIS });
	} catch (cause) {
		const reason = cause instanceof Error ? cause.message : String(cause);
		throw new Error(`extracting the icon from ${apkPath} failed: ${reason}`, { cause });
	}
}

async function readIcon(path: string): Promise<Buffer | null> {
	try {
		return await readFile(path);
	} catch (cause) {
		if (isMissingFile(cause)) return null;
		throw cause;
	}
}

function isMissingFile(cause: unknown): boolean {
	return cause instanceof Error && 'code' in cause && cause.code === 'ENOENT';
}
