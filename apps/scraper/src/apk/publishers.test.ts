import { describe, expect, it } from 'vitest';
import publishers from './platform-publishers.json';
import { PLATFORM_CERTIFICATES } from './publishers.ts';

const ARCORE_ORIGINAL = '6e913f65ea53321ead6932ff777e1b7f94bbba7e2b0dafdd8494b100101e3797';
const ARCORE_ROTATED = '44ec33984cbe5eccf282a7e499dfd98665a8fd6be503eb5a7181a5326e226b9e';

describe('platform publishers', () => {
	it('lists every certificate as a lowercase sha-256 fingerprint', () => {
		for (const fingerprint of PLATFORM_CERTIFICATES) {
			expect(fingerprint).toMatch(/^[0-9a-f]{64}$/);
		}
	});

	it('lists each certificate once', () => {
		expect(new Set(PLATFORM_CERTIFICATES).size).toBe(PLATFORM_CERTIFICATES.length);
	});

	it('records where each certificate was verified', () => {
		for (const certificate of publishers.flatMap((publisher) => publisher.certificates)) {
			expect(certificate.verifiedIn).toMatch(/^[a-z][a-z0-9_]*(\.[a-z0-9_]+)+$/i);
		}
	});

	it("covers both keys in ARCore's rotation", () => {
		expect(PLATFORM_CERTIFICATES).toEqual(
			expect.arrayContaining([ARCORE_ORIGINAL, ARCORE_ROTATED])
		);
	});
});
