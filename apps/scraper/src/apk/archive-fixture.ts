import { deflateRawSync } from 'node:zlib';
import { certificate, type CertificateName } from './certificate-fixtures.ts';
import { bytesOf, children, CONTEXT_0, readTlv } from './der.ts';

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

export type ZipOptions = { comment?: string; beforeDirectory?: Buffer };

export function buildZip(entries: ArchiveEntry[], options: string | ZipOptions = {}): Buffer {
	const { comment = '', beforeDirectory = Buffer.alloc(0) } = zipOptions(options);
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
	eocd.writeUInt32LE(offset + beforeDirectory.length, 16);
	eocd.writeUInt16LE(trailer.length, 20);
	trailer.copy(eocd, 22);

	return Buffer.concat([...locals, beforeDirectory, directory, eocd]);
}

function zipOptions(options: string | ZipOptions): ZipOptions {
	return typeof options === 'string' ? { comment: options } : options;
}

export const V2_ID = 0x7109871a;
export const V3_ID = 0xf05368c0;
export const V31_ID = 0x1b93ad61;

export function buildSigningBlock(pairs: { id: number; value: Buffer }[]): Buffer {
	const body = Buffer.concat(
		pairs.map((pair) => Buffer.concat([uint64(4 + pair.value.length), uint32(pair.id), pair.value]))
	);
	const size = uint64(body.length + 24);

	return Buffer.concat([size, body, size, Buffer.from('APK Sig Block 42', 'latin1')]);
}

export function v2Signers(certificates: Buffer[][]): Buffer {
	return sequence(
		certificates.map((chain) =>
			Buffer.concat([
				prefixed(Buffer.concat([sequence([]), sequence(chain), sequence([])])),
				sequence([]),
				prefixed(Buffer.from('public-key'))
			])
		)
	);
}

export type V3Fixture = {
	certificate: Buffer;
	minSdk?: number;
	maxSdk?: number;
	lineage?: Buffer[];
};

export function v3Signers(signers: V3Fixture[]): Buffer {
	return sequence(
		signers.map((signer) => {
			const bounds = Buffer.concat([
				uint32(signer.minSdk ?? 24),
				uint32(signer.maxSdk ?? 0x7fffffff)
			]);
			const attributes = signer.lineage === undefined ? [] : [rotationAttribute(signer.lineage)];
			const signedData = Buffer.concat([
				sequence([]),
				sequence([signer.certificate]),
				bounds,
				sequence(attributes)
			]);

			return Buffer.concat([
				prefixed(signedData),
				bounds,
				sequence([]),
				prefixed(Buffer.from('public-key'))
			]);
		})
	);
}

function rotationAttribute(certificates: Buffer[]): Buffer {
	const nodes = certificates.map((certificate) =>
		prefixed(
			Buffer.concat([
				prefixed(Buffer.concat([prefixed(certificate), uint32(0x0103)])),
				uint32(0),
				uint32(0x0103),
				prefixed(Buffer.from('signature'))
			])
		)
	);

	return Buffer.concat([uint32(0x3ba06f8c), uint32(1), ...nodes]);
}

function sequence(items: Buffer[]): Buffer {
	return prefixed(Buffer.concat(items.map(prefixed)));
}

function prefixed(value: Buffer): Buffer {
	return Buffer.concat([uint32(value.length), value]);
}

function uint32(value: number): Buffer {
	const buffer = Buffer.alloc(4);
	buffer.writeUInt32LE(value, 0);
	return buffer;
}

function uint64(value: number): Buffer {
	const buffer = Buffer.alloc(8);
	buffer.writeBigUInt64LE(BigInt(value), 0);
	return buffer;
}

export type CertificateFixture = { encoded: Buffer; issuer: Buffer; serial: Buffer };

export function certificateFixture(name: CertificateName): CertificateFixture {
	const encoded = certificate(name);
	const [tbs] = children(encoded, readTlv(encoded, 0)!) ?? [];
	const fields = children(encoded, tbs!) ?? [];
	const offset = fields[0]?.tag === CONTEXT_0 ? 1 : 0;

	return {
		encoded,
		serial: bytesOf(encoded, fields[offset]!),
		issuer: bytesOf(encoded, fields[offset + 2]!)
	};
}

export function buildPkcs7(certificates: CertificateFixture[], signer: CertificateFixture): Buffer {
	const signerInfo = der(
		0x30,
		der(0x02, Buffer.from([1])),
		der(0x30, signer.issuer, signer.serial)
	);
	const signedData = der(
		0x30,
		der(0x02, Buffer.from([1])),
		der(0x31),
		der(0x30, der(0x06, Buffer.from([0x2a, 0x86, 0x48]))),
		der(0xa0, ...certificates.map((certificate) => certificate.encoded)),
		der(0x31, signerInfo)
	);

	return der(0x30, der(0x06, Buffer.from([0x2a, 0x86, 0x48])), der(0xa0, signedData));
}

function der(tag: number, ...content: Buffer[]): Buffer {
	const body = Buffer.concat(content);
	const length =
		body.length < 0x80
			? Buffer.from([body.length])
			: body.length < 0x100
				? Buffer.from([0x81, body.length])
				: Buffer.from([0x82, body.length >> 8, body.length & 0xff]);

	return Buffer.concat([Buffer.from([tag]), length, body]);
}
