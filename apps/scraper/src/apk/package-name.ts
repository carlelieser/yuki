import { inflateRawSync } from 'node:zlib';
import { readManifestPackage } from './axml.ts';
import { findEntry, readEntry, type RangeReader } from './zip.ts';

const MANIFEST_ENTRY = 'AndroidManifest.xml';
const MAX_MANIFEST = 4 * 1024 * 1024;

const PACKAGE_PATTERN = /^[a-z][a-z0-9_]*(\.[a-z0-9_]+)+$/i;

export type ApkSource = {
	size: () => Promise<number | null>;
	read: RangeReader;
};

export async function readApkPackageName(source: ApkSource): Promise<string | null> {
	const size = await source.size();
	if (size === null || size <= 0) return null;

	const entry = await findEntry(size, source.read, MANIFEST_ENTRY);
	if (entry === null) return null;
	if (entry.uncompressedSize > MAX_MANIFEST) return null;

	const raw = await readEntry(entry, size, source.read);
	const manifest = entry.isDeflated ? inflateRawSync(raw, { maxOutputLength: MAX_MANIFEST }) : raw;

	const packageName = readManifestPackage(manifest);
	if (packageName === null) return null;

	return isPlausible(packageName) ? packageName : null;
}

function isPlausible(value: string): boolean {
	return PACKAGE_PATTERN.test(value) && value.split('.').length >= 2;
}
