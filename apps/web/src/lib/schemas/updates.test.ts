import { describe, expect, it } from 'vitest';
import { MAX_UPDATE_CHECK_ENTRIES, updateCheckSchema } from './updates.ts';

const entry = { packageName: 'dev.yuki.sample', versionTag: '1.2.3', versionCode: '12' };

describe('updateCheckSchema', () => {
	it('accepts a complete request', () => {
		const result = updateCheckSchema.safeParse({ installed: [entry], includePrereleases: true });

		expect(result.success).toBe(true);
		expect(result.data?.installed).toEqual([entry]);
		expect(result.data?.includePrereleases).toBe(true);
	});

	it('defaults to an empty release-only check', () => {
		const result = updateCheckSchema.safeParse({});

		expect(result.success).toBe(true);
		expect(result.data?.installed).toEqual([]);
		expect(result.data?.includePrereleases).toBe(false);
	});

	it('defaults a missing version code to null', () => {
		const result = updateCheckSchema.safeParse({
			installed: [{ packageName: 'dev.yuki.sample', versionTag: '1.2.3' }]
		});

		expect(result.success).toBe(true);
		expect(result.data?.installed[0]?.versionCode).toBeNull();
	});

	it('rejects package names that are not Android identifiers', () => {
		const result = updateCheckSchema.safeParse({
			installed: [{ ...entry, packageName: 'not-a-package' }]
		});

		expect(result.success).toBe(false);
	});

	it('rejects an empty version tag', () => {
		const result = updateCheckSchema.safeParse({ installed: [{ ...entry, versionTag: '' }] });

		expect(result.success).toBe(false);
	});

	it('caps how many installed apps one request may carry', () => {
		const oversized = Array.from({ length: MAX_UPDATE_CHECK_ENTRIES + 1 }, (_, index) => ({
			...entry,
			packageName: `dev.yuki.sample${index}`
		}));

		expect(updateCheckSchema.safeParse({ installed: oversized }).success).toBe(false);
	});

	it('accepts a request at the cap', () => {
		const sized = Array.from({ length: MAX_UPDATE_CHECK_ENTRIES }, (_, index) => ({
			...entry,
			packageName: `dev.yuki.sample${index}`
		}));

		expect(updateCheckSchema.safeParse({ installed: sized }).success).toBe(true);
	});

	it('trims surrounding whitespace', () => {
		const result = updateCheckSchema.safeParse({
			installed: [
				{ packageName: '  dev.yuki.sample  ', versionTag: '  1.2.3  ', versionCode: null }
			]
		});

		expect(result.data?.installed[0]?.packageName).toBe('dev.yuki.sample');
		expect(result.data?.installed[0]?.versionTag).toBe('1.2.3');
	});
});
