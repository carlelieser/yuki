import { describe, expect, it } from 'vitest';
import { isNewerTag, parseVersion } from './version-compare.ts';

describe('parseVersion', () => {
	it('reads a plain semantic version', () => {
		expect(parseVersion('1.2.3')).toEqual({ release: [1, 2, 3], prerelease: null });
	});

	it('ignores a leading v and other prefixes', () => {
		expect(parseVersion('v1.2.3')).toEqual({ release: [1, 2, 3], prerelease: null });
		expect(parseVersion('release-2.0')).toEqual({ release: [2, 0], prerelease: null });
	});

	it('reads a prerelease suffix', () => {
		expect(parseVersion('1.2.3-beta.1')).toEqual({ release: [1, 2, 3], prerelease: ['beta', '1'] });
	});

	it('discards build metadata', () => {
		expect(parseVersion('1.2.3+build.5')).toEqual({ release: [1, 2, 3], prerelease: null });
	});

	it('returns null for tags carrying no version', () => {
		expect(parseVersion('latest')).toBeNull();
		expect(parseVersion('')).toBeNull();
	});
});

describe('isNewerTag', () => {
	it('detects a higher release', () => {
		expect(isNewerTag('1.2.4', '1.2.3')).toBe(true);
		expect(isNewerTag('2.0.0', '1.9.9')).toBe(true);
	});

	it('rejects the same or an older release', () => {
		expect(isNewerTag('1.2.3', '1.2.3')).toBe(false);
		expect(isNewerTag('1.2.2', '1.2.3')).toBe(false);
	});

	it('compares numeric parts rather than strings', () => {
		expect(isNewerTag('1.10.0', '1.9.0')).toBe(true);
		expect(isNewerTag('1.9.0', '1.10.0')).toBe(false);
	});

	it('treats missing trailing parts as zero', () => {
		expect(isNewerTag('1.2', '1.2.0')).toBe(false);
		expect(isNewerTag('1.2.1', '1.2')).toBe(true);
	});

	it('ranks a release above its own prereleases', () => {
		expect(isNewerTag('1.2.3', '1.2.3-beta.1')).toBe(true);
		expect(isNewerTag('1.2.3-beta.1', '1.2.3')).toBe(false);
	});

	it('orders prereleases against each other', () => {
		expect(isNewerTag('1.2.3-beta.2', '1.2.3-beta.1')).toBe(true);
		expect(isNewerTag('1.2.3-rc.1', '1.2.3-beta.1')).toBe(true);
		expect(isNewerTag('1.2.3-beta.1', '1.2.3-rc.1')).toBe(false);
	});

	it('compares tags written with differing prefixes', () => {
		expect(isNewerTag('v1.2.4', '1.2.3')).toBe(true);
	});

	it('never reports an update when either tag is unparseable', () => {
		expect(isNewerTag('latest', '1.2.3')).toBe(false);
		expect(isNewerTag('1.2.4', 'nightly')).toBe(false);
		expect(isNewerTag('nightly', 'latest')).toBe(false);
	});

	it('reports no update for identical unparseable tags', () => {
		expect(isNewerTag('nightly', 'nightly')).toBe(false);
	});
});
