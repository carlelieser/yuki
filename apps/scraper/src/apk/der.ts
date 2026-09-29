export type Tlv = {
	tag: number;
	start: number;
	contentStart: number;
	end: number;
};

export const SEQUENCE = 0x30;
export const SET = 0x31;
export const INTEGER = 0x02;
export const CONTEXT_0 = 0xa0;

export function readTlv(buffer: Buffer, offset: number, limit = buffer.length): Tlv | null {
	if (offset + 2 > limit) return null;

	const tag = buffer[offset]!;
	if ((tag & 0x1f) === 0x1f) return null;

	const first = buffer[offset + 1]!;
	let contentStart = offset + 2;
	let length = first;

	if (first & 0x80) {
		const count = first & 0x7f;
		if (count === 0 || count > 4 || contentStart + count > limit) return null;

		length = 0;
		for (let index = 0; index < count; index += 1) {
			length = length * 256 + buffer[contentStart + index]!;
		}
		contentStart += count;
	}

	const end = contentStart + length;
	if (end > limit) return null;

	return { tag, start: offset, contentStart, end };
}

export function children(buffer: Buffer, parent: Tlv): Tlv[] | null {
	const items: Tlv[] = [];
	let cursor = parent.contentStart;

	while (cursor < parent.end) {
		const item = readTlv(buffer, cursor, parent.end);
		if (item === null) return null;

		items.push(item);
		cursor = item.end;
	}

	return items;
}

export function bytesOf(buffer: Buffer, tlv: Tlv): Buffer {
	return buffer.subarray(tlv.start, tlv.end);
}
