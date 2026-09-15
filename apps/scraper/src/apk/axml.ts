const STRING_POOL_TYPE = 0x0001;
const START_ELEMENT_TYPE = 0x0102;

const UTF8_FLAG = 1 << 8;

const HEADER_SIZE = 8;
const STRING_POOL_HEADER = 28;
const ATTRIBUTE_HEADER = 16;

const NO_ENTRY = 0xffffffff;

export function readManifestPackage(manifest: Buffer): string | null {
	if (manifest.length < HEADER_SIZE + STRING_POOL_HEADER) return null;
	if (manifest.readUInt16LE(HEADER_SIZE) !== STRING_POOL_TYPE) return null;

	const strings = readStringPool(manifest);
	if (strings === null) return null;

	return readPackageAttribute(manifest, strings);
}

function readStringPool(manifest: Buffer): string[] | null {
	const base = HEADER_SIZE;
	const count = manifest.readUInt32LE(base + 8);
	const stringsStart = manifest.readUInt32LE(base + 20);
	const isUtf8 = (manifest.readUInt32LE(base + 16) & UTF8_FLAG) !== 0;

	const offsetsStart = base + STRING_POOL_HEADER;
	if (offsetsStart + count * 4 > manifest.length) return null;

	const strings: string[] = [];

	for (let index = 0; index < count; index += 1) {
		const offset = base + stringsStart + manifest.readUInt32LE(offsetsStart + index * 4);
		if (offset < 0 || offset >= manifest.length) return null;

		const value = isUtf8 ? readUtf8(manifest, offset) : readUtf16(manifest, offset);
		if (value === null) return null;

		strings.push(value);
	}

	return strings;
}

function readUtf8(manifest: Buffer, offset: number): string | null {
	let length = manifest[offset + 1];
	if (length === undefined) return null;

	let start = offset + 2;
	if ((length & 0x80) !== 0) {
		const next = manifest[offset + 2];
		if (next === undefined) return null;

		length = ((length & 0x7f) << 8) | next;
		start = offset + 3;
	}

	if (start + length > manifest.length) return null;

	return manifest.toString('utf8', start, start + length);
}

function readUtf16(manifest: Buffer, offset: number): string | null {
	let length = manifest.readUInt16LE(offset);
	let start = offset + 2;

	if ((length & 0x8000) !== 0) {
		length = ((length & 0x7fff) << 16) | manifest.readUInt16LE(offset + 2);
		start = offset + 4;
	}

	if (start + length * 2 > manifest.length) return null;

	return manifest.toString('utf16le', start, start + length * 2);
}

function readPackageAttribute(manifest: Buffer, strings: string[]): string | null {
	let cursor = HEADER_SIZE + manifest.readUInt32LE(HEADER_SIZE + 4);

	while (cursor + HEADER_SIZE <= manifest.length) {
		const type = manifest.readUInt16LE(cursor);
		const size = manifest.readUInt32LE(cursor + 4);
		if (size <= 0) return null;

		if (type === START_ELEMENT_TYPE && strings[manifest.readUInt32LE(cursor + 20)] === 'manifest') {
			return findAttribute(manifest, strings, cursor);
		}

		cursor += size;
	}

	return null;
}

function findAttribute(manifest: Buffer, strings: string[], element: number): string | null {
	const attributeStart = manifest.readUInt16LE(element + 24);
	const attributeSize = manifest.readUInt16LE(element + 26);
	const attributeCount = manifest.readUInt16LE(element + 28);

	for (let index = 0; index < attributeCount; index += 1) {
		const attribute = element + ATTRIBUTE_HEADER + attributeStart + index * attributeSize;
		if (attribute + ATTRIBUTE_HEADER > manifest.length) return null;

		if (strings[manifest.readUInt32LE(attribute + 4)] !== 'package') continue;

		const raw = manifest.readUInt32LE(attribute + 8);
		const value = raw !== NO_ENTRY ? strings[raw] : strings[manifest.readUInt32LE(attribute + 16)];

		return value ?? null;
	}

	return null;
}
