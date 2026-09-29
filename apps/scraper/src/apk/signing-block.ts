import { ApkFormatError } from './format-error.ts';
import type { ZipArchive } from './zip.ts';

const MAGIC = Buffer.from('APK Sig Block 42', 'latin1');
const FOOTER_SIZE = 24;
const MIN_BLOCK_SIZE = 32;
const MAX_BLOCK_SIZE = 16 * 1024 * 1024;
const PAIR_HEADER_SIZE = 12;

export type SigningBlock = Map<number, Buffer>;

type ByteRange = { start: number; end: number };

export async function readSigningBlock(archive: ZipArchive): Promise<SigningBlock | null> {
	const end = archive.directoryOffset;
	if (end < FOOTER_SIZE) return null;

	const footer = await slice(archive, { start: end - FOOTER_SIZE, end });
	if (!footer.subarray(8).equals(MAGIC)) return null;

	const blockSize = readSize(footer, 0);
	const isSizeSane = blockSize !== null && blockSize >= MIN_BLOCK_SIZE;
	if (!isSizeSane || blockSize > MAX_BLOCK_SIZE || blockSize + 8 > end) {
		throw new ApkFormatError(`reading the signing block found an invalid size ${blockSize}`);
	}

	const block = await slice(archive, { start: end - blockSize - 8, end });
	if (readSize(block, 0) !== blockSize) {
		throw new ApkFormatError('reading the signing block found mismatched sizes');
	}

	return readPairs(block.subarray(8, block.length - FOOTER_SIZE));
}

async function slice(archive: ZipArchive, range: ByteRange): Promise<Buffer> {
	if (range.start >= archive.tailStart) {
		return archive.tail.subarray(range.start - archive.tailStart, range.end - archive.tailStart);
	}

	return archive.read(range.start, range.end - 1);
}

function readPairs(pairs: Buffer): SigningBlock {
	const block: SigningBlock = new Map();
	let cursor = 0;

	while (cursor < pairs.length) {
		const length = readSize(pairs, cursor);
		const pairEnd = length === null ? pairs.length + 1 : cursor + 8 + length;
		if (length === null || length < 4 || pairEnd > pairs.length) {
			throw new ApkFormatError(`reading the signing block pair at ${cursor} ran out of bounds`);
		}

		const id = pairs.readUInt32LE(cursor + 8);
		const value = pairs.subarray(cursor + PAIR_HEADER_SIZE, pairEnd);
		if (!block.has(id)) block.set(id, value);

		cursor = pairEnd;
	}

	return block;
}

function readSize(buffer: Buffer, offset: number): number | null {
	if (offset + 8 > buffer.length) return null;

	const size = buffer.readBigUInt64LE(offset);
	return size > BigInt(Number.MAX_SAFE_INTEGER) ? null : Number(size);
}
