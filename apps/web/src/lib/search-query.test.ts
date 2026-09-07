import { describe, expect, it } from 'vitest';
import {
	hasEnoughLengthForTrigram,
	MAX_QUERY_LENGTH,
	MAX_SEARCH_OFFSET,
	normalizeSearchQuery,
	readOffset,
	toPrefixTsQuery
} from './search-query.ts';

const WELL_FORMED_TSQUERY = /^[a-zA-Z0-9._-]+(?: & [a-zA-Z0-9._-]+)*(?::\*)?$/;

describe('normalizeSearchQuery', () => {
	it('trims surrounding whitespace', () => {
		expect(normalizeSearchQuery('  file explorer  ')).toBe('file explorer');
	});

	it('collapses runs of whitespace into single spaces', () => {
		expect(normalizeSearchQuery('file \t\n  explorer')).toBe('file explorer');
	});

	it('returns an empty string for blank and non-string input', () => {
		expect(normalizeSearchQuery('')).toBe('');
		expect(normalizeSearchQuery('   ')).toBe('');
		expect(normalizeSearchQuery(undefined)).toBe('');
		expect(normalizeSearchQuery(null)).toBe('');
		expect(normalizeSearchQuery(42)).toBe('');
		expect(normalizeSearchQuery(['file'])).toBe('');
	});

	it('caps overlong input without throwing', () => {
		const normalized = normalizeSearchQuery('a'.repeat(10_000));
		expect(normalized).toHaveLength(MAX_QUERY_LENGTH);
	});
});

describe('toPrefixTsQuery', () => {
	it('marks a single token as a prefix match', () => {
		expect(toPrefixTsQuery('note')).toBe('note:*');
	});

	it('ands tokens together and prefixes only the last', () => {
		expect(toPrefixTsQuery('dark note')).toBe('dark & note:*');
	});

	it('preserves characters that repository names rely on', () => {
		expect(toPrefixTsQuery('my-plugin')).toBe('my-plugin:*');
		expect(toPrefixTsQuery('my_plugin')).toBe('my_plugin:*');
		expect(toPrefixTsQuery('my.plugin')).toBe('my.plugin:*');
	});

	it('strips tsquery operators from every input', () => {
		const hostile = ['foo & bar', 'foo | bar', '!foo', '(foo)', "foo'bar", 'foo:*', 'foo <-> bar'];
		for (const input of hostile) {
			expect(toPrefixTsQuery(normalizeSearchQuery(input))).toMatch(WELL_FORMED_TSQUERY);
		}
	});

	it('returns an empty string when every token is stripped', () => {
		expect(toPrefixTsQuery('')).toBe('');
		expect(toPrefixTsQuery('& | !')).toBe('');
	});
});

describe('hasEnoughLengthForTrigram', () => {
	it('rejects queries shorter than three characters', () => {
		expect(hasEnoughLengthForTrigram('')).toBe(false);
		expect(hasEnoughLengthForTrigram('a')).toBe(false);
		expect(hasEnoughLengthForTrigram('ab')).toBe(false);
	});

	it('accepts queries of three characters or more', () => {
		expect(hasEnoughLengthForTrigram('abc')).toBe(true);
		expect(hasEnoughLengthForTrigram('abcd')).toBe(true);
	});
});

describe('readOffset', () => {
	it('reads a valid positive offset', () => {
		expect(readOffset('24')).toBe(24);
	});

	it('treats missing and unparseable values as the first page', () => {
		expect(readOffset(null)).toBe(0);
		expect(readOffset('')).toBe(0);
		expect(readOffset('abc')).toBe(0);
		expect(readOffset('NaN')).toBe(0);
	});

	it('rejects negative offsets', () => {
		expect(readOffset('-1')).toBe(0);
		expect(readOffset('-9999')).toBe(0);
	});

	it('clamps offsets beyond the maximum', () => {
		expect(readOffset('999999')).toBe(MAX_SEARCH_OFFSET);
	});

	it('floors fractional offsets', () => {
		expect(readOffset('24.9')).toBe(24);
	});
});
