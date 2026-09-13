import { describe, expect, it } from 'vitest';
import { isEtagUsable } from './sources.ts';
import { MAPPING_VERSION } from '../mapping/version.ts';

describe('isEtagUsable', () => {
	it('keeps a stored etag that was written by the current mapping', () => {
		expect(isEtagUsable(MAPPING_VERSION)).toBe(true);
	});

	it('discards a stored etag from an older mapping so the resource is remapped', () => {
		expect(isEtagUsable(MAPPING_VERSION - 1)).toBe(false);
	});

	it('discards the default version carried by rows that predate mapping versions', () => {
		expect(isEtagUsable(0)).toBe(false);
	});

	it('discards an etag from a newer mapping, as after a rollback', () => {
		expect(isEtagUsable(MAPPING_VERSION + 1)).toBe(false);
	});
});
