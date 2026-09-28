import { describe, expect, it } from 'vitest';
import { createHash } from 'node:crypto';
import {
	buildCertificate,
	buildManifest,
	buildPkcs7,
	buildSigningBlock,
	buildZip,
	v2Signers,
	v3Signers,
	V2_ID,
	V31_ID,
	V3_ID,
	type ArchiveEntry
} from './archive-fixture.ts';
import { ApkFormatError } from './format-error.ts';
import { readApkIdentity, type ApkSource } from './identity.ts';

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

async function readApkPackageName(source: ApkSource): Promise<string | null> {
	return (await readApkIdentity(source))?.packageName ?? null;
}

function digest(certificate: Buffer): string {
	return createHash('sha256').update(certificate).digest('hex');
}

const OLD_KEY = Buffer.from('certificate-old');
const NEW_KEY = Buffer.from('certificate-new');
const OTHER_KEY = Buffer.from('certificate-other');

function signedApk(beforeDirectory: Buffer, entries: ArchiveEntry[] = []): Buffer {
	return buildZip([manifestEntry('com.acme.app', true), ...entries], { beforeDirectory });
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

describe('readApkIdentity', () => {
	it('reads the signer out of a v2 signature', async () => {
		const block = buildSigningBlock([{ id: V2_ID, value: v2Signers([[NEW_KEY]]) }]);

		expect(await readApkIdentity(sourceFor(signedApk(block)))).toEqual({
			packageName: 'com.acme.app',
			signers: [digest(NEW_KEY)],
			lineage: []
		});
	});

	it('keeps every signer of an apk signed by several keys', async () => {
		const block = buildSigningBlock([{ id: V2_ID, value: v2Signers([[NEW_KEY], [OTHER_KEY]]) }]);
		const identity = await readApkIdentity(sourceFor(signedApk(block)));

		expect(identity?.signers).toEqual([digest(NEW_KEY), digest(OTHER_KEY)].sort());
	});

	it('prefers the v3 signer and records the keys it rotated away from', async () => {
		const block = buildSigningBlock([
			{ id: V2_ID, value: v2Signers([[OLD_KEY]]) },
			{ id: V3_ID, value: v3Signers([{ certificate: NEW_KEY, lineage: [OLD_KEY, NEW_KEY] }]) }
		]);

		expect(await readApkIdentity(sourceFor(signedApk(block)))).toEqual({
			packageName: 'com.acme.app',
			signers: [digest(NEW_KEY)],
			lineage: [digest(OLD_KEY)]
		});
	});

	it('takes the v3.1 signer as current and keeps the v3 signer as lineage', async () => {
		const block = buildSigningBlock([
			{ id: V3_ID, value: v3Signers([{ certificate: OLD_KEY, maxSdk: 32 }]) },
			{ id: V31_ID, value: v3Signers([{ certificate: NEW_KEY, minSdk: 33 }]) }
		]);
		const identity = await readApkIdentity(sourceFor(signedApk(block)));

		expect(identity?.signers).toEqual([digest(NEW_KEY)]);
		expect(identity?.lineage).toEqual([digest(OLD_KEY)]);
	});

	it('falls back to the jar signature of an apk with no signing block', async () => {
		const signer = buildCertificate('CN=acme', 7);
		const authority = buildCertificate('CN=authority', 9);
		const signature = buildPkcs7([authority, signer], signer);
		const archive = buildZip([
			manifestEntry('com.acme.app', true),
			{ name: 'META-INF/CERT.RSA', body: signature, deflate: true }
		]);

		expect(await readApkIdentity(sourceFor(archive))).toEqual({
			packageName: 'com.acme.app',
			signers: [digest(signer.encoded)],
			lineage: []
		});
	});

	it('reads a signing block that lies outside the tail', async () => {
		const block = buildSigningBlock([
			{ id: V2_ID, value: v2Signers([[NEW_KEY]]) },
			{ id: 0x42726577, value: Buffer.alloc(80 * 1024, 0) }
		]);
		const identity = await readApkIdentity(sourceFor(signedApk(block)));

		expect(identity?.signers).toEqual([digest(NEW_KEY)]);
	});

	it('reads the signer without reading the whole archive', async () => {
		const padding = { name: 'payload.bin', body: Buffer.alloc(512 * 1024, 9), deflate: false };
		const block = buildSigningBlock([{ id: V2_ID, value: v2Signers([[NEW_KEY]]) }]);
		const archive = buildZip([padding, manifestEntry('com.acme.app', true)], {
			beforeDirectory: block
		});
		const calls: number[] = [];

		const identity = await readApkIdentity(sourceFor(archive, calls));

		expect(identity?.signers).toEqual([digest(NEW_KEY)]);
		expect(calls.reduce((total, length) => total + length, 0)).toBeLessThan(archive.length / 2);
	});

	it('treats bytes without the block magic as no signing block', async () => {
		const block = buildSigningBlock([{ id: V2_ID, value: v2Signers([[NEW_KEY]]) }]);
		block.write('APK Sig Block 41', block.length - 16, 'latin1');

		expect(await readApkIdentity(sourceFor(signedApk(block)))).toEqual({
			packageName: 'com.acme.app',
			signers: [],
			lineage: []
		});
	});

	it('rejects a block whose leading and trailing sizes disagree', async () => {
		const block = buildSigningBlock([{ id: V2_ID, value: v2Signers([[NEW_KEY]]) }]);
		block.writeBigUInt64LE(BigInt(block.length), 0);

		await expect(readApkIdentity(sourceFor(signedApk(block)))).rejects.toThrow(ApkFormatError);
	});

	it('rejects a v2 signature it cannot parse rather than calling the apk unsigned', async () => {
		const block = buildSigningBlock([{ id: V2_ID, value: Buffer.from([8, 0, 0, 0, 1, 2]) }]);

		await expect(readApkIdentity(sourceFor(signedApk(block)))).rejects.toThrow(ApkFormatError);
	});

	it('rejects a jar signature that names no certificate it carries', async () => {
		const signer = buildCertificate('CN=acme', 7);
		const stranger = buildCertificate('CN=stranger', 8);
		const archive = buildZip([
			manifestEntry('com.acme.app', true),
			{ name: 'META-INF/CERT.RSA', body: buildPkcs7([stranger], signer), deflate: true }
		]);

		await expect(readApkIdentity(sourceFor(archive))).rejects.toThrow(ApkFormatError);
	});

	it('returns no signers for an unsigned apk', async () => {
		const archive = apkWith([manifestEntry('com.acme.app', true)]);

		expect((await readApkIdentity(sourceFor(archive)))?.signers).toEqual([]);
	});
});
