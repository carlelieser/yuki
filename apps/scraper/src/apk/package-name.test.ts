import { describe, expect, it } from 'vitest';
import { buildManifest, buildZip, type ArchiveEntry } from './archive-fixture.ts';
import { readApkPackageName, type ApkSource } from './package-name.ts';

function sourceFor(archive: Buffer, calls: number[] = []): ApkSource {
	return {
		size: async () => archive.length,
		read: async (start, end) => {
			calls.push(end - start + 1);
			return archive.subarray(start, end + 1);
		}
	};
}

function apkWith(entries: ArchiveEntry[], comment = ''): Buffer {
	return buildZip(entries, comment);
}

function manifestEntry(packageName: string, deflate: boolean): ArchiveEntry {
	return { name: 'AndroidManifest.xml', body: buildManifest(packageName), deflate };
}

describe('readApkPackageName', () => {
	it('reads the package out of a deflated manifest', async () => {
		const archive = apkWith([manifestEntry('dev.imranr.obtainium.fdroid', true)]);

		expect(await readApkPackageName(sourceFor(archive))).toBe('dev.imranr.obtainium.fdroid');
	});

	it('reads the package out of a stored manifest', async () => {
		const archive = apkWith([manifestEntry('moe.shizuku.privileged.api', false)]);

		expect(await readApkPackageName(sourceFor(archive))).toBe('moe.shizuku.privileged.api');
	});

	it('reads a manifest whose string pool is utf16', async () => {
		const manifest = buildManifest('com.looker.droidify', 'utf16');
		const archive = apkWith([{ name: 'AndroidManifest.xml', body: manifest, deflate: true }]);

		expect(await readApkPackageName(sourceFor(archive))).toBe('com.looker.droidify');
	});

	it('finds the manifest among other entries', async () => {
		const archive = apkWith([
			{ name: 'classes.dex', body: Buffer.alloc(4096, 7), deflate: true },
			manifestEntry('com.topjohnwu.magisk', true),
			{ name: 'resources.arsc', body: Buffer.alloc(2048, 3), deflate: false }
		]);

		expect(await readApkPackageName(sourceFor(archive))).toBe('com.topjohnwu.magisk');
	});

	it('finds the manifest when a zip comment pushes the directory out of the tail', async () => {
		const archive = apkWith([manifestEntry('me.bmax.apatch', true)], 'x'.repeat(1024));

		expect(await readApkPackageName(sourceFor(archive))).toBe('me.bmax.apatch');
	});

	it('never reads the whole archive', async () => {
		const padding = { name: 'payload.bin', body: Buffer.alloc(512 * 1024, 9), deflate: false };
		const archive = apkWith([padding, manifestEntry('io.github.muntashirakon.AppManager', true)]);
		const calls: number[] = [];

		await readApkPackageName(sourceFor(archive, calls));

		const read = calls.reduce((total, length) => total + length, 0);
		expect(read).toBeLessThan(archive.length / 2);
	});

	it('returns null when there is no manifest', async () => {
		const archive = apkWith([{ name: 'classes.dex', body: Buffer.alloc(64, 1), deflate: true }]);

		expect(await readApkPackageName(sourceFor(archive))).toBeNull();
	});

	it('returns null when the size is unknown', async () => {
		const archive = apkWith([manifestEntry('com.termux', true)]);
		const source = { ...sourceFor(archive), size: async () => null };

		expect(await readApkPackageName(source)).toBeNull();
	});

	it('returns null for a package name that is not plausible', async () => {
		const archive = apkWith([manifestEntry('notapackage', true)]);

		expect(await readApkPackageName(sourceFor(archive))).toBeNull();
	});

	it('returns null rather than throwing on a truncated archive', async () => {
		const archive = apkWith([manifestEntry('com.termux', true)]).subarray(0, 40);

		expect(await readApkPackageName(sourceFor(archive))).toBeNull();
	});

	it('returns null for an archive that is not a zip', async () => {
		const archive = Buffer.alloc(2048, 0);

		expect(await readApkPackageName(sourceFor(archive))).toBeNull();
	});
});
