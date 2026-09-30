import { X509Certificate } from 'node:crypto';
import { ApkFormatError } from './format-error.ts';

const PLATFORM_PUBLISHERS = [{ organization: 'Google Inc.' }, { organization: 'Google LLC' }];

const PLATFORM_ORGANIZATIONS = new Set(
	PLATFORM_PUBLISHERS.map((publisher) => `O=${publisher.organization}`)
);

export function isPlatformCertificate(certificate: Buffer): boolean {
	return subjectOf(certificate)
		.split('\n')
		.some((attribute) => PLATFORM_ORGANIZATIONS.has(attribute));
}

function subjectOf(certificate: Buffer): string {
	try {
		return new X509Certificate(certificate).subject;
	} catch (cause) {
		throw new ApkFormatError('reading the signer certificate subject failed', { cause });
	}
}
