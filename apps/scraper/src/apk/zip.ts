export type RangeReader = (start: number, end: number) => Promise<Buffer>;

export type ZipEntry = {
	offset: number;
	compressedSize: number;
	uncompressedSize: number;
	isDeflated: boolean;
};

const EOCD_SIGNATURE = 0x06054b50;
const CENTRAL_HEADER_SIGNATURE = 0x02014b50;

const EOCD_SIZE = 22;
const MAX_COMMENT = 65535;
const CENTRAL_HEADER_SIZE = 46;
const LOCAL_HEADER_SIZE = 30;

const ZIP64_SENTINEL = 0xffffffff;
const MAX_CENTRAL_DIRECTORY = 4 * 1024 * 1024;

const STORED = 0;
const DEFLATED = 8;

export async function findEntry(
	size: number,
	read: RangeReader,
	wanted: string
): Promise<ZipEntry | null> {
	if (size < EOCD_SIZE) return null;

	const tailSize = Math.min(EOCD_SIZE + MAX_COMMENT, size);
	const tailStart = size - tailSize;
	const tail = await read(tailStart, size - 1);

	const eocd = locateEocd(tail);
	if (eocd === null) return null;

	const directorySize = tail.readUInt32LE(eocd + 12);
	const directoryOffset = tail.readUInt32LE(eocd + 16);
	if (directorySize === ZIP64_SENTINEL || directoryOffset === ZIP64_SENTINEL) return null;
	if (directorySize > MAX_CENTRAL_DIRECTORY) return null;
	if (directoryOffset + directorySize > size) return null;

	const directory = await readDirectory(read, tail, tailStart, directoryOffset, directorySize);

	return scanDirectory(directory, wanted, size);
}

function locateEocd(tail: Buffer): number | null {
	for (let index = tail.length - EOCD_SIZE; index >= 0; index -= 1) {
		if (tail.readUInt32LE(index) === EOCD_SIGNATURE) return index;
	}

	return null;
}

async function readDirectory(
	read: RangeReader,
	tail: Buffer,
	tailStart: number,
	offset: number,
	length: number
): Promise<Buffer> {
	if (offset >= tailStart) {
		const start = offset - tailStart;
		return tail.subarray(start, start + length);
	}

	return read(offset, offset + length - 1);
}

function scanDirectory(directory: Buffer, wanted: string, size: number): ZipEntry | null {
	let cursor = 0;

	while (cursor + CENTRAL_HEADER_SIZE <= directory.length) {
		if (directory.readUInt32LE(cursor) !== CENTRAL_HEADER_SIGNATURE) return null;

		const method = directory.readUInt16LE(cursor + 10);
		const compressedSize = directory.readUInt32LE(cursor + 20);
		const uncompressedSize = directory.readUInt32LE(cursor + 24);
		const nameLength = directory.readUInt16LE(cursor + 28);
		const extraLength = directory.readUInt16LE(cursor + 30);
		const commentLength = directory.readUInt16LE(cursor + 32);
		const offset = directory.readUInt32LE(cursor + 42);

		const nameStart = cursor + CENTRAL_HEADER_SIZE;
		const name = directory.toString('utf8', nameStart, nameStart + nameLength);

		if (name === wanted) {
			if (method !== STORED && method !== DEFLATED) return null;
			if (compressedSize === 0 || compressedSize === ZIP64_SENTINEL) return null;
			if (offset === ZIP64_SENTINEL || offset + compressedSize > size) return null;

			return {
				offset,
				compressedSize,
				uncompressedSize,
				isDeflated: method === DEFLATED
			};
		}

		cursor = nameStart + nameLength + extraLength + commentLength;
	}

	return null;
}

export async function readEntry(entry: ZipEntry, size: number, read: RangeReader): Promise<Buffer> {
	const headerEnd = Math.min(entry.offset + LOCAL_HEADER_SIZE - 1, size - 1);
	const header = await read(entry.offset, headerEnd);
	if (header.length < LOCAL_HEADER_SIZE) {
		throw new Error('truncated local header');
	}

	const nameLength = header.readUInt16LE(26);
	const extraLength = header.readUInt16LE(28);
	const dataStart = entry.offset + LOCAL_HEADER_SIZE + nameLength + extraLength;
	const dataEnd = dataStart + entry.compressedSize - 1;
	if (dataEnd >= size) throw new Error('entry runs past the end of the archive');

	return read(dataStart, dataEnd);
}
