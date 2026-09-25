import { deflateSync, inflateSync } from 'node:zlib';

export type RgbaImage = {
	width: number;
	height: number;
	pixels: Buffer;
};

const SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
const CHANNELS = 4;
const HEADER_LENGTH = 8;
const CRC_LENGTH = 4;

const GREYSCALE = 0;
const TRUECOLOUR = 2;
const INDEXED = 3;
const GREYSCALE_ALPHA = 4;
const TRUECOLOUR_ALPHA = 6;

const SUPPORTED_DEPTH = 8;
const MAX_PIXELS = 4096 * 4096;

type Chunks = { header: Buffer; data: Buffer; palette: Buffer | null; alpha: Buffer | null };

function readChunks(source: Buffer): Chunks | null {
	if (!source.subarray(0, SIGNATURE.length).equals(SIGNATURE)) return null;

	let header: Buffer | null = null;
	let palette: Buffer | null = null;
	let alpha: Buffer | null = null;
	const parts: Buffer[] = [];

	let offset = SIGNATURE.length;
	while (offset + HEADER_LENGTH <= source.length) {
		const length = source.readUInt32BE(offset);
		const type = source.toString('ascii', offset + 4, offset + 8);
		const start = offset + HEADER_LENGTH;
		if (start + length > source.length) return null;

		const body = source.subarray(start, start + length);
		if (type === 'IHDR') header = body;
		else if (type === 'PLTE') palette = body;
		else if (type === 'tRNS') alpha = body;
		else if (type === 'IDAT') parts.push(body);
		else if (type === 'IEND') break;

		offset = start + length + CRC_LENGTH;
	}

	if (header === null || parts.length === 0) return null;
	return { header, data: Buffer.concat(parts), palette, alpha };
}

function paeth(left: number, above: number, corner: number): number {
	const estimate = left + above - corner;
	const fromLeft = Math.abs(estimate - left);
	const fromAbove = Math.abs(estimate - above);
	const fromCorner = Math.abs(estimate - corner);

	if (fromLeft <= fromAbove && fromLeft <= fromCorner) return left;
	return fromAbove <= fromCorner ? above : corner;
}

function undoFilter(filter: number, line: Buffer, previous: Buffer, step: number): void {
	for (let index = 0; index < line.length; index += 1) {
		const left = index >= step ? (line[index - step] ?? 0) : 0;
		const above = previous[index] ?? 0;
		const corner = index >= step ? (previous[index - step] ?? 0) : 0;
		const value = line[index] ?? 0;

		if (filter === 1) line[index] = (value + left) & 0xff;
		else if (filter === 2) line[index] = (value + above) & 0xff;
		else if (filter === 3) line[index] = (value + ((left + above) >> 1)) & 0xff;
		else if (filter === 4) line[index] = (value + paeth(left, above, corner)) & 0xff;
	}
}

function channelsFor(colourType: number): number | null {
	if (colourType === GREYSCALE) return 1;
	if (colourType === TRUECOLOUR) return 3;
	if (colourType === INDEXED) return 1;
	if (colourType === GREYSCALE_ALPHA) return 2;
	if (colourType === TRUECOLOUR_ALPHA) return 4;
	return null;
}

function expandPixel(
	source: Buffer,
	offset: number,
	colourType: number,
	lookup: { palette: Buffer | null; alpha: Buffer | null }
): [number, number, number, number] {
	const first = source[offset] ?? 0;

	if (colourType === TRUECOLOUR_ALPHA) {
		return [first, source[offset + 1] ?? 0, source[offset + 2] ?? 0, source[offset + 3] ?? 0];
	}
	if (colourType === TRUECOLOUR) {
		return [first, source[offset + 1] ?? 0, source[offset + 2] ?? 0, 0xff];
	}
	if (colourType === GREYSCALE_ALPHA) {
		return [first, first, first, source[offset + 1] ?? 0];
	}
	if (colourType === GREYSCALE) {
		return [first, first, first, 0xff];
	}

	const palette = lookup.palette;
	if (palette === null) return [0, 0, 0, 0];

	const base = first * 3;
	const opacity = lookup.alpha?.[first] ?? 0xff;
	return [palette[base] ?? 0, palette[base + 1] ?? 0, palette[base + 2] ?? 0, opacity];
}

export function decodePng(source: Buffer): RgbaImage | null {
	const chunks = readChunks(source);
	if (chunks === null) return null;

	const width = chunks.header.readUInt32BE(0);
	const height = chunks.header.readUInt32BE(4);
	const depth = chunks.header.readUInt8(8);
	const colourType = chunks.header.readUInt8(9);
	const interlace = chunks.header.readUInt8(12);

	const channels = channelsFor(colourType);
	if (channels === null || depth !== SUPPORTED_DEPTH || interlace !== 0) return null;
	if (width === 0 || height === 0 || width * height > MAX_PIXELS) return null;

	const raw = inflateSync(chunks.data);
	const step = channels;
	const stride = width * channels;
	const pixels = Buffer.alloc(width * height * CHANNELS);

	let previous = Buffer.alloc(stride);
	let cursor = 0;

	for (let row = 0; row < height; row += 1) {
		if (cursor + 1 + stride > raw.length) return null;

		const filter = raw[cursor] ?? 0;
		const line = Buffer.from(raw.subarray(cursor + 1, cursor + 1 + stride));
		cursor += 1 + stride;

		undoFilter(filter, line, previous, step);

		for (let column = 0; column < width; column += 1) {
			const [red, green, blue, opacity] = expandPixel(line, column * channels, colourType, {
				palette: chunks.palette,
				alpha: chunks.alpha
			});
			const target = (row * width + column) * CHANNELS;
			pixels[target] = red;
			pixels[target + 1] = green;
			pixels[target + 2] = blue;
			pixels[target + 3] = opacity;
		}

		previous = line;
	}

	return { width, height, pixels };
}

function crc32(source: Buffer): number {
	let remainder = 0xffffffff;

	for (const byte of source) {
		remainder ^= byte;
		for (let bit = 0; bit < 8; bit += 1) {
			remainder = remainder & 1 ? (remainder >>> 1) ^ 0xedb88320 : remainder >>> 1;
		}
	}

	return (remainder ^ 0xffffffff) >>> 0;
}

function chunk(type: string, body: Buffer): Buffer {
	const length = Buffer.alloc(4);
	length.writeUInt32BE(body.length);

	const tagged = Buffer.concat([Buffer.from(type, 'ascii'), body]);
	const checksum = Buffer.alloc(4);
	checksum.writeUInt32BE(crc32(tagged));

	return Buffer.concat([length, tagged, checksum]);
}

export function encodePng(image: RgbaImage): Buffer {
	const header = Buffer.alloc(13);
	header.writeUInt32BE(image.width, 0);
	header.writeUInt32BE(image.height, 4);
	header.writeUInt8(SUPPORTED_DEPTH, 8);
	header.writeUInt8(TRUECOLOUR_ALPHA, 9);

	const stride = image.width * CHANNELS;
	const raw = Buffer.alloc((stride + 1) * image.height);

	for (let row = 0; row < image.height; row += 1) {
		const target = row * (stride + 1);
		raw[target] = 0;
		image.pixels.copy(raw, target + 1, row * stride, (row + 1) * stride);
	}

	return Buffer.concat([
		SIGNATURE,
		chunk('IHDR', header),
		chunk('IDAT', deflateSync(raw)),
		chunk('IEND', Buffer.alloc(0))
	]);
}
