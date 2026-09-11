import { describe, expect, it } from 'vitest';
import { BROWSE_SORT_OPTIONS, defaultOrderFor } from './browse.ts';
import {
	DEFAULT_SEARCH_SORTING,
	fromSearchSortValue,
	hasEnoughLengthForTrigram,
	isRelevanceSort,
	MAX_QUERY_LENGTH,
	MAX_SEARCH_OFFSET,
	MAX_SEARCH_PAGE_SIZE,
	normalizeSearchQuery,
	readOffset,
	readSearchLimit,
	readSearchSorting,
	SEARCH_PAGE_SIZE,
	SEARCH_RELEVANCE_SORT,
	SEARCH_SORT_OPTIONS,
	toPrefixTsQuery,
	toSearchQueryString,
	toSearchSortValue
} from './search-query.ts';
import { CATEGORY_OPTIONS, readCategory } from './categories.ts';

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

describe('readSearchLimit', () => {
	it('reads a valid positive limit', () => {
		expect(readSearchLimit('12')).toBe(12);
	});

	it('falls back to the default page size for missing and unparseable values', () => {
		expect(readSearchLimit(null)).toBe(SEARCH_PAGE_SIZE);
		expect(readSearchLimit('')).toBe(SEARCH_PAGE_SIZE);
		expect(readSearchLimit('abc')).toBe(SEARCH_PAGE_SIZE);
		expect(readSearchLimit('0')).toBe(SEARCH_PAGE_SIZE);
		expect(readSearchLimit('-5')).toBe(SEARCH_PAGE_SIZE);
	});

	it('clamps a limit beyond the maximum', () => {
		expect(readSearchLimit('999999')).toBe(MAX_SEARCH_PAGE_SIZE);
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

describe('SEARCH_SORT_OPTIONS', () => {
	it('leads with relevance and reuses the browse options', () => {
		expect(SEARCH_SORT_OPTIONS[0]?.value).toBe(SEARCH_RELEVANCE_SORT);
		expect(SEARCH_SORT_OPTIONS.length).toBe(BROWSE_SORT_OPTIONS.length + 1);
		expect(SEARCH_SORT_OPTIONS.slice(1)).toEqual(BROWSE_SORT_OPTIONS);
	});

	it('exposes unique values', () => {
		const values = SEARCH_SORT_OPTIONS.map((option) => option.value);
		expect(new Set(values).size).toBe(values.length);
	});
});

describe('isRelevanceSort', () => {
	it('identifies the relevance sort', () => {
		expect(isRelevanceSort(SEARCH_RELEVANCE_SORT)).toBe(true);
		expect(isRelevanceSort('stars')).toBe(false);
	});
});

describe('toSearchSortValue', () => {
	it('collapses relevance to a single value', () => {
		expect(toSearchSortValue({ sort: SEARCH_RELEVANCE_SORT, order: 'desc' })).toBe(
			SEARCH_RELEVANCE_SORT
		);
	});

	it('pairs a column sort with its order', () => {
		expect(toSearchSortValue({ sort: 'stars', order: 'asc' })).toBe('stars-asc');
		expect(toSearchSortValue({ sort: 'name', order: 'desc' })).toBe('name-desc');
	});
});

describe('fromSearchSortValue', () => {
	it('round-trips every option', () => {
		for (const option of SEARCH_SORT_OPTIONS) {
			const sorting = fromSearchSortValue(option.value);
			expect(sorting).toEqual({ sort: option.sort, order: option.order });
			expect(toSearchSortValue(sorting)).toBe(option.value);
		}
	});

	it('falls back to relevance for unknown values', () => {
		expect(fromSearchSortValue('bogus')).toEqual(DEFAULT_SEARCH_SORTING);
	});
});

describe('readSearchSorting', () => {
	it('defaults to relevance when no sort is given', () => {
		expect(readSearchSorting(new URLSearchParams())).toEqual(DEFAULT_SEARCH_SORTING);
	});

	it('reads relevance explicitly', () => {
		expect(readSearchSorting(new URLSearchParams({ sort: SEARCH_RELEVANCE_SORT }))).toEqual(
			DEFAULT_SEARCH_SORTING
		);
	});

	it('reads a column sort with its order', () => {
		expect(readSearchSorting(new URLSearchParams({ sort: 'stars', order: 'asc' }))).toEqual({
			sort: 'stars',
			order: 'asc'
		});
	});

	it('falls back to the browse default order for a known sort', () => {
		expect(readSearchSorting(new URLSearchParams({ sort: 'name' }))).toEqual({
			sort: 'name',
			order: defaultOrderFor('name')
		});
		expect(readSearchSorting(new URLSearchParams({ sort: 'updated', order: 'sideways' }))).toEqual({
			sort: 'updated',
			order: defaultOrderFor('updated')
		});
	});

	it('falls back to relevance for an unknown sort', () => {
		expect(readSearchSorting(new URLSearchParams({ sort: 'bogus', order: 'asc' }))).toEqual(
			DEFAULT_SEARCH_SORTING
		);
	});

	it('ignores the order when the sort is relevance', () => {
		expect(
			readSearchSorting(new URLSearchParams({ sort: SEARCH_RELEVANCE_SORT, order: 'asc' }))
		).toEqual(DEFAULT_SEARCH_SORTING);
	});
});

describe('toSearchQueryString', () => {
	it('emits only the query for the default relevance sort', () => {
		expect(toSearchQueryString('hello world', DEFAULT_SEARCH_SORTING)).toBe('q=hello%20world');
	});

	it('includes the sort and order for a column sort', () => {
		expect(toSearchQueryString('tetris', { sort: 'stars', order: 'asc' })).toBe(
			'q=tetris&sort=stars&order=asc'
		);
	});

	it('appends a positive offset last', () => {
		expect(toSearchQueryString('tetris', { sort: 'name', order: 'desc' }, 24)).toBe(
			'q=tetris&sort=name&order=desc&offset=24'
		);
	});

	it('omits a zero offset', () => {
		expect(toSearchQueryString('tetris', DEFAULT_SEARCH_SORTING, 0)).toBe('q=tetris');
	});

	it('encodes characters that would break the query string', () => {
		expect(toSearchQueryString('a&b=c d', DEFAULT_SEARCH_SORTING)).toBe('q=a%26b%3Dc%20d');
	});

	it('round-trips through readSearchSorting', () => {
		for (const option of SEARCH_SORT_OPTIONS) {
			const sorting = { sort: option.sort, order: option.order };
			const params = new URLSearchParams(toSearchQueryString('tetris', sorting));
			expect(params.get('q')).toBe('tetris');
			expect(readSearchSorting(params)).toEqual(sorting);
		}
	});

	it('appends a category after the offset', () => {
		expect(toSearchQueryString('tetris', DEFAULT_SEARCH_SORTING, 24, 'gaming')).toBe(
			'q=tetris&offset=24&category=gaming'
		);
	});

	it('omits a null category', () => {
		expect(toSearchQueryString('tetris', DEFAULT_SEARCH_SORTING, 0, null)).toBe('q=tetris');
	});

	it('keeps the category alongside a column sort', () => {
		expect(toSearchQueryString('tetris', { sort: 'stars', order: 'desc' }, 0, 'media')).toBe(
			'q=tetris&sort=stars&order=desc&category=media'
		);
	});

	it('round-trips the category through readCategory', () => {
		for (const option of CATEGORY_OPTIONS) {
			const params = new URLSearchParams(
				toSearchQueryString('tetris', DEFAULT_SEARCH_SORTING, 0, option.value)
			);
			expect(readCategory(params.get('category'))).toBe(option.value);
		}
	});
});
