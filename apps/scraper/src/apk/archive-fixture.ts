import { deflateRawSync } from 'node:zlib';

type PoolEncoding = 'utf8' | 'utf16';

export function buildManifest(packageName: string, encoding: PoolEncoding = 'utf8'): Buffer {
	const strings = ['package', 'manifest', packageName];
	const pool = buildStringPool(strings, encoding);
	const element = buildStartElement({
		nameIndex: 1,
		attributeNameIndex: 0,
		attributeValueIndex: 2
	});

	const body = Buffer.concat([pool, element]);
	const header = Buffer.alloc(8);
	header.writeUInt16LE(0x0003, 0);
	header.writeUInt16LE(8, 2);
	header.writeUInt32LE(8 + body.length, 4);

	return Buffer.concat([header, body]);
}

function buildStringPool(strings: string[], encoding: PoolEncoding): Buffer {
	const encoded = strings.map((value) =>
		encoding === 'utf8' ? encodeUtf8(value) : encodeUtf16(value)
	);

	const offsets = Buffer.alloc(strings.length * 4);
	let cursor = 0;
	encoded.forEach((entry, index) => {
		offsets.writeUInt32LE(cursor, index * 4);
		cursor += entry.length;
	});

	const data = Buffer.concat(encoded);
	const headerSize = 28;
	const size = headerSize + offsets.length + data.length;

	const header = Buffer.alloc(headerSize);
	header.writeUInt16LE(0x0001, 0);
	header.writeUInt16LE(headerSize, 2);
	header.writeUInt32LE(size, 4);
	header.writeUInt32LE(strings.length, 8);
	header.writeUInt32LE(0, 12);
	header.writeUInt32LE(encoding === 'utf8' ? 1 << 8 : 0, 16);
	header.writeUInt32LE(headerSize + offsets.length, 20);
	header.writeUInt32LE(0, 24);

	return Buffer.concat([header, offsets, data]);
}

function encodeUtf8(value: string): Buffer {
	const bytes = Buffer.from(value, 'utf8');
	return Buffer.concat([Buffer.from([value.length, bytes.length]), bytes, Buffer.from([0])]);
}

function encodeUtf16(value: string): Buffer {
	const bytes = Buffer.from(value, 'utf16le');
	const length = Buffer.alloc(2);
	length.writeUInt16LE(value.length, 0);

	return Buffer.concat([length, bytes, Buffer.from([0, 0])]);
}

type ElementInput = {
	nameIndex: number;
	attributeNameIndex: number;
	attributeValueIndex: number;
};

function buildStartElement(input: ElementInput): Buffer {
	const attributeSize = 20;
	const size = 36 + attributeSize;
	const element = Buffer.alloc(size);

	element.writeUInt16LE(0x0102, 0);
	element.writeUInt16LE(16, 2);
	element.writeUInt32LE(size, 4);
	element.writeUInt32LE(0, 8);
	element.writeUInt32LE(0xffffffff, 12);
	element.writeUInt32LE(0xffffffff, 16);
	element.writeUInt32LE(input.nameIndex, 20);
	element.writeUInt16LE(20, 24);
	element.writeUInt16LE(attributeSize, 26);
	element.writeUInt16LE(1, 28);

	const attribute = 36;
	element.writeUInt32LE(0xffffffff, attribute);
	element.writeUInt32LE(input.attributeNameIndex, attribute + 4);
	element.writeUInt32LE(input.attributeValueIndex, attribute + 8);
	element.writeUInt32LE(0x03000008, attribute + 12);
	element.writeUInt32LE(input.attributeValueIndex, attribute + 16);

	return element;
}

export type ArchiveEntry = { name: string; body: Buffer; deflate: boolean };

export function buildZip(entries: ArchiveEntry[], comment = ''): Buffer {
	const locals: Buffer[] = [];
	const centrals: Buffer[] = [];
	let offset = 0;

	for (const entry of entries) {
		const name = Buffer.from(entry.name, 'utf8');
		const stored = entry.deflate ? deflateRawSync(entry.body) : entry.body;

		const local = Buffer.alloc(30 + name.length);
		local.writeUInt32LE(0x04034b50, 0);
		local.writeUInt16LE(20, 4);
		local.writeUInt16LE(entry.deflate ? 8 : 0, 8);
		local.writeUInt32LE(stored.length, 18);
		local.writeUInt32LE(entry.body.length, 22);
		local.writeUInt16LE(name.length, 26);
		name.copy(local, 30);

		const central = Buffer.alloc(46 + name.length);
		central.writeUInt32LE(0x02014b50, 0);
		central.writeUInt16LE(20, 6);
		central.writeUInt16LE(entry.deflate ? 8 : 0, 10);
		central.writeUInt32LE(stored.length, 20);
		central.writeUInt32LE(entry.body.length, 24);
		central.writeUInt16LE(name.length, 28);
		central.writeUInt32LE(offset, 42);
		name.copy(central, 46);

		locals.push(local, stored);
		centrals.push(central);
		offset += local.length + stored.length;
	}

	const directory = Buffer.concat(centrals);
	const trailer = Buffer.from(comment, 'utf8');
	const eocd = Buffer.alloc(22 + trailer.length);
	eocd.writeUInt32LE(0x06054b50, 0);
	eocd.writeUInt16LE(entries.length, 8);
	eocd.writeUInt16LE(entries.length, 10);
	eocd.writeUInt32LE(directory.length, 12);
	eocd.writeUInt32LE(offset, 16);
	eocd.writeUInt16LE(trailer.length, 20);
	trailer.copy(eocd, 22);

	return Buffer.concat([...locals, directory, eocd]);
}
