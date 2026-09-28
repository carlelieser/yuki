import { bytesOf, children, CONTEXT_0, INTEGER, readTlv, SEQUENCE, SET, type Tlv } from './der.ts';

type Certificate = {
	encoded: Buffer;
	issuer: Buffer;
	serial: Buffer;
};

export function signerCertificate(signature: Buffer): Buffer | null {
	const contentInfo = readTlv(signature, 0);
	if (contentInfo === null || contentInfo.tag !== SEQUENCE) return null;

	const [, explicit] = children(signature, contentInfo) ?? [];
	if (explicit === undefined || explicit.tag !== CONTEXT_0) return null;

	const [signedData] = children(signature, explicit) ?? [];
	if (signedData === undefined || signedData.tag !== SEQUENCE) return null;

	const fields = children(signature, signedData);
	if (fields === null) return null;

	const certificateSet = fields.find((field) => field.tag === CONTEXT_0);
	const signerInfos = fields.at(-1);
	if (certificateSet === undefined || signerInfos === undefined || signerInfos.tag !== SET) {
		return null;
	}

	const certificates = (children(signature, certificateSet) ?? [])
		.map((tlv) => readCertificate(signature, tlv))
		.filter((certificate): certificate is Certificate => certificate !== null);

	const [signerInfo] = children(signature, signerInfos) ?? [];
	const identifier = signerInfo === undefined ? null : issuerAndSerial(signature, signerInfo);
	if (identifier === null) return null;

	const match = certificates.find(
		(certificate) =>
			certificate.issuer.equals(identifier.issuer) && certificate.serial.equals(identifier.serial)
	);

	return match?.encoded ?? null;
}

function readCertificate(buffer: Buffer, tlv: Tlv): Certificate | null {
	if (tlv.tag !== SEQUENCE) return null;

	const [tbs] = children(buffer, tlv) ?? [];
	if (tbs === undefined || tbs.tag !== SEQUENCE) return null;

	const fields = children(buffer, tbs);
	if (fields === null) return null;

	const offset = fields[0]?.tag === CONTEXT_0 ? 1 : 0;
	const serial = fields[offset];
	const issuer = fields[offset + 2];
	if (serial?.tag !== INTEGER || issuer?.tag !== SEQUENCE) return null;

	return {
		encoded: bytesOf(buffer, tlv),
		issuer: bytesOf(buffer, issuer),
		serial: bytesOf(buffer, serial)
	};
}

function issuerAndSerial(
	buffer: Buffer,
	signerInfo: Tlv
): { issuer: Buffer; serial: Buffer } | null {
	if (signerInfo.tag !== SEQUENCE) return null;

	const [, identifier] = children(buffer, signerInfo) ?? [];
	if (identifier === undefined || identifier.tag !== SEQUENCE) return null;

	const [issuer, serial] = children(buffer, identifier) ?? [];
	if (issuer?.tag !== SEQUENCE || serial?.tag !== INTEGER) return null;

	return { issuer: bytesOf(buffer, issuer), serial: bytesOf(buffer, serial) };
}
