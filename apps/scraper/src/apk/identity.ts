import { inflateRawSync } from 'node:zlib';
import { readManifestPackage } from './axml.ts';
import { ApkFormatError } from './format-error.ts';
import { signerCertificate } from './pkcs7.ts';
import { signersFromBlock, signersFromCertificates, UNSIGNED, type Signers } from './signers.ts';
import { readSigningBlock } from './signing-block.ts';
import { openArchive, readEntry, type RangeReader, type ZipArchive, type ZipEntry } from './zip.ts';

const MANIFEST_ENTRY = 'AndroidManifest.xml';
const MAX_MANIFEST = 4 * 1024 * 1024;
const MAX_SIGNATURE_FILE = 256 * 1024;
const MAX_SIGNATURE_FILES = 8;

const PACKAGE_PATTERN = /^[a-z][a-z0-9_]*(\.[a-z0-9_]+)+$/i;
const SIGNATURE_FILE_PATTERN = /^META-INF\/[^/]+\.(RSA|DSA|EC)$/i;

export type ApkSource = {
	size: () => Promise<number | null>;
	read: RangeReader;
};

export type ApkIdentity = Signers & {
	packageName: string;
};

export async function readApkIdentity(source: ApkSource): Promise<ApkIdentity | null> {
	const size = await source.size();
	if (size === null || size <= 0) return null;

	const archive = await openArchive(size, source.read);
	if (archive === null) return null;

	const packageName = await readPackageName(archive);
	if (packageName === null) return null;

	return { packageName, ...(await readSigners(archive)) };
}

async function readPackageName(archive: ZipArchive): Promise<string | null> {
	const entry = archive.entries.get(MANIFEST_ENTRY);
	if (entry == null) return null;

	const manifest = await inflatedEntry(archive, entry, MAX_MANIFEST);
	const packageName = manifest === null ? null : readManifestPackage(manifest);
	if (packageName === null) return null;

	return isPlausible(packageName) ? packageName : null;
}

async function readSigners(archive: ZipArchive): Promise<Signers> {
	const block = await readSigningBlock(archive);
	const fromBlock = block === null ? null : signersFromBlock(block);

	return fromBlock ?? readJarSigners(archive);
}

async function readJarSigners(archive: ZipArchive): Promise<Signers> {
	const files = [...archive.entries]
		.filter(([name]) => SIGNATURE_FILE_PATTERN.test(name))
		.slice(0, MAX_SIGNATURE_FILES);
	if (files.length === 0) return UNSIGNED;

	const certificates: Buffer[] = [];
	for (const [name, entry] of files) {
		const signature =
			entry === null ? null : await inflatedEntry(archive, entry, MAX_SIGNATURE_FILE);
		const certificate = signature === null ? null : signerCertificate(signature);
		if (certificate === null) {
			throw new ApkFormatError(`reading the jar signature ${name} found no signer certificate`);
		}

		certificates.push(certificate);
	}

	return signersFromCertificates(certificates);
}

async function inflatedEntry(
	archive: ZipArchive,
	entry: ZipEntry,
	limit: number
): Promise<Buffer | null> {
	if (entry.uncompressedSize > limit) return null;

	const raw = await readEntry(entry, archive.size, archive.read);
	if (!entry.isDeflated) return raw;

	try {
		return inflateRawSync(raw, { maxOutputLength: limit });
	} catch (cause) {
		throw new ApkFormatError(`inflating the entry at ${entry.offset} failed`, { cause });
	}
}

function isPlausible(value: string): boolean {
	return PACKAGE_PATTERN.test(value) && value.split('.').length >= 2;
}
