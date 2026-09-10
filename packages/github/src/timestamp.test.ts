import { describe, expect, it } from 'vitest';
import { parseTimestamp } from './timestamp.ts';

describe('parseTimestamp', () => {
	it('parses a GitHub timestamp', () => {
		expect(parseTimestamp('2026-02-01T00:00:00Z')).toEqual(new Date('2026-02-01T00:00:00Z'));
	});

	it('treats null and undefined alike, since search results omit fields', () => {
		expect(parseTimestamp(null)).toBeNull();
		expect(parseTimestamp(undefined)).toBeNull();
	});

	it('returns null rather than an Invalid Date the database would reject', () => {
		expect(parseTimestamp('not a date')).toBeNull();
	});
});
