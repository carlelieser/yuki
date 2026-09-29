import { createHash } from 'node:crypto';
import { ApkFormatError } from './format-error.ts';
import type { SigningBlock } from './signing-block.ts';

export const V2_BLOCK_ID = 0x7109871a;
export const V3_BLOCK_ID = 0xf05368c0;
export const V31_BLOCK_ID = 0x1b93ad61;
export const ROTATION_ATTRIBUTE_ID = 0x3ba06f8c;

const LINEAGE_VERSION = 1;
const V3_BLOCK_IDS = [V31_BLOCK_ID, V3_BLOCK_ID];

export type Signers = {
	signers: string[];
	lineage: string[];
};

export const UNSIGNED: Signers = { signers: [], lineage: [] };

type V3Signer = {
	certificate: Buffer;
	maxSdk: number;
	lineage: Buffer[];
};

export function signersFromBlock(block: SigningBlock): Signers | null {
	const v3 = V3_BLOCK_IDS.flatMap((id) => {
		const value = block.get(id);
		return value === undefined ? [] : readV3Signers(value);
	});
	if (v3.length > 0) return v3Identity(v3);

	const v2 = block.get(V2_BLOCK_ID);
	if (v2 === undefined) return null;

	return { signers: distinctDigests(readV2Signers(v2)), lineage: [] };
}

export function signersFromCertificates(certificates: Buffer[]): Signers {
	return { signers: distinctDigests(certificates), lineage: [] };
}

export function certificateDigest(certificate: Buffer): string {
	return createHash('sha256').update(certificate).digest('hex');
}

function v3Identity(signers: V3Signer[]): Signers {
	const newest = signers.reduce((best, signer) => (signer.maxSdk > best.maxSdk ? signer : best));
	const current = certificateDigest(newest.certificate);
	const others = signers.flatMap((signer) => [signer.certificate, ...signer.lineage]);

	return {
		signers: [current],
		lineage: distinctDigests(others).filter((digest) => digest !== current)
	};
}

function readV2Signers(value: Buffer): Buffer[] {
	return blockSigners(value, 'v2').map((signer) => {
		const signedData = required(new Reader(signer).prefixed(), 'v2 signed data');
		return firstCertificate(new Reader(signedData));
	});
}

function readV3Signers(value: Buffer): V3Signer[] {
	return blockSigners(value, 'v3').map(readV3Signer);
}

function readV3Signer(signer: Buffer): V3Signer {
	const reader = new Reader(required(new Reader(signer).prefixed(), 'v3 signed data'));
	const certificate = firstCertificate(reader);

	required(reader.uint32(), 'v3 minimum sdk');
	const maxSdk = required(reader.uint32(), 'v3 maximum sdk');
	const attributes = sequence(reader.prefixed(), 'v3 attributes');

	return { certificate, maxSdk, lineage: attributes.flatMap(rotationCertificates) };
}

function firstCertificate(signedData: Reader): Buffer {
	required(signedData.prefixed(), 'signer digests');

	const [certificate] = sequence(signedData.prefixed(), 'signer certificates');
	return required(certificate ?? null, 'signer certificate');
}

function rotationCertificates(attribute: Buffer): Buffer[] {
	const reader = new Reader(attribute);
	if (reader.uint32() !== ROTATION_ATTRIBUTE_ID) return [];
	if (reader.uint32() !== LINEAGE_VERSION) return [];

	const certificates: Buffer[] = [];
	while (reader.hasRemaining()) {
		const node = required(reader.prefixed(), 'rotation node');
		const signedData = required(new Reader(node).prefixed(), 'rotation signed data');
		certificates.push(required(new Reader(signedData).prefixed(), 'rotation certificate'));
	}

	return certificates;
}

function blockSigners(value: Buffer, scheme: string): Buffer[] {
	const signers = sequence(new Reader(value).prefixed(), `${scheme} signers`);
	if (signers.length === 0) throw new ApkFormatError(`reading ${scheme} signers found none`);

	return signers;
}

function sequence(content: Buffer | null, field: string): Buffer[] {
	const reader = new Reader(required(content, field));
	const items: Buffer[] = [];
	while (reader.hasRemaining()) {
		items.push(required(reader.prefixed(), field));
	}

	return items;
}

function required<Value>(value: Value | null, field: string): Value {
	if (value === null) throw new ApkFormatError(`reading the ${field} ran out of bounds`);

	return value;
}

function distinctDigests(certificates: Buffer[]): string[] {
	return [...new Set(certificates.map(certificateDigest))].sort();
}

class Reader {
	private cursor = 0;

	constructor(private readonly buffer: Buffer) {}

	hasRemaining(): boolean {
		return this.cursor < this.buffer.length;
	}

	uint32(): number | null {
		if (this.cursor + 4 > this.buffer.length) return null;

		const value = this.buffer.readUInt32LE(this.cursor);
		this.cursor += 4;
		return value;
	}

	prefixed(): Buffer | null {
		const length = this.uint32();
		if (length === null || this.cursor + length > this.buffer.length) return null;

		const value = this.buffer.subarray(this.cursor, this.cursor + length);
		this.cursor += length;
		return value;
	}
}
