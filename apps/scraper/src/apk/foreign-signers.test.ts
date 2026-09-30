import { describe, expect, it } from 'vitest';
import {
	buildManifest,
	buildPkcs7,
	buildSigningBlock,
	buildZip,
	certificateFixture,
	v2Signers,
	v3Signers,
	V2_ID,
	V3_ID,
	type ArchiveEntry
} from './archive-fixture.ts';
import { certificate } from './certificate-fixtures.ts';
import { ApkFormatError } from './format-error.ts';
import { readApkIdentity, type ApkSource } from './identity.ts';

function sourceFor(archive: Buffer): ApkSource {
	return {
		size: async () => archive.length,
		read: async (start, end) => archive.subarray(start, end + 1)
	};
}

function signedApk(pairs: { id: number; value: Buffer }[], entries: ArchiveEntry[] = []): Buffer {
	const manifest = {
		name: 'AndroidManifest.xml',
		body: buildManifest('com.acme.app'),
		deflate: true
	};
	return buildZip([manifest, ...entries], { beforeDirectory: buildSigningBlock(pairs) });
}

async function isForeign(archive: Buffer): Promise<boolean | undefined> {
	return (await readApkIdentity(sourceFor(archive)))?.isForeign;
}

describe('foreign signers', () => {
	it('marks an apk signed by Google Inc. as foreign', async () => {
		const archive = signedApk([{ id: V2_ID, value: v2Signers([[certificate('google')]]) }]);

		expect(await isForeign(archive)).toBe(true);
	});

	it('marks an apk signed by Google LLC as foreign', async () => {
		const archive = signedApk([{ id: V2_ID, value: v2Signers([[certificate('googlellc')]]) }]);

		expect(await isForeign(archive)).toBe(true);
	});

	it('keeps an apk signed by its own developer', async () => {
		const archive = signedApk([{ id: V2_ID, value: v2Signers([[certificate('new')]]) }]);

		expect(await isForeign(archive)).toBe(false);
	});

	it('keeps an apk signed by Google and another key together', async () => {
		const archive = signedApk([
			{ id: V2_ID, value: v2Signers([[certificate('google')], [certificate('new')]]) }
		]);

		expect(await isForeign(archive)).toBe(false);
	});

	it('judges a rotated apk by its current signer, not its lineage', async () => {
		const rotatedAway = signedApk([
			{
				id: V3_ID,
				value: v3Signers([{ certificate: certificate('new'), lineage: [certificate('google')] }])
			}
		]);
		const rotatedTo = signedApk([
			{
				id: V3_ID,
				value: v3Signers([{ certificate: certificate('google'), lineage: [certificate('old')] }])
			}
		]);

		expect(await isForeign(rotatedAway)).toBe(false);
		expect(await isForeign(rotatedTo)).toBe(true);
	});

	it('marks a jar-signed apk signed by Google as foreign', async () => {
		const signer = certificateFixture('google');
		const archive = buildZip([
			{ name: 'AndroidManifest.xml', body: buildManifest('com.acme.app'), deflate: true },
			{ name: 'META-INF/CERT.RSA', body: buildPkcs7([signer], signer), deflate: true }
		]);

		expect(await isForeign(archive)).toBe(true);
	});

	it('treats an unsigned apk as its own', async () => {
		const archive = buildZip([
			{ name: 'AndroidManifest.xml', body: buildManifest('com.acme.app'), deflate: true }
		]);

		expect(await isForeign(archive)).toBe(false);
	});

	it('rejects a signer certificate it cannot parse', async () => {
		const archive = signedApk([
			{ id: V2_ID, value: v2Signers([[Buffer.from('not a certificate')]]) }
		]);

		await expect(readApkIdentity(sourceFor(archive))).rejects.toThrow(ApkFormatError);
	});
});
