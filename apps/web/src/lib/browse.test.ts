import { describe, expect, it } from 'vitest';
import {
	BROWSE_SORT_OPTIONS,
	DEFAULT_BROWSE_SORT,
	defaultOrderFor,
	fromSortValue,
	isFeaturedRequested,
	MAX_BROWSE_OFFSET,
	readBrowseOffset,
	readBrowseOrder,
	readBrowseSort,
	readBrowseSorting,
	toBrowseQueryString,
	toSortValue
} from './browse.ts';

describe('readBrowseOffset', () => {
	it('reads a valid positive offset', () => {
		expect(readBrowseOffset('24')).toBe(24);
	});

	it('treats missing and unparseable values as the first page', () => {
		expect(readBrowseOffset(null)).toBe(0);
		expect(readBrowseOffset('')).toBe(0);
		expect(readBrowseOffset('abc')).toBe(0);
	});

	it('clamps offsets beyond the maximum', () => {
		expect(readBrowseOffset('999999')).toBe(MAX_BROWSE_OFFSET);
	});
});

describe('readBrowseSort', () => {
	it('accepts every supported sort', () => {
		expect(readBrowseSort('stars')).toBe('stars');
		expect(readBrowseSort('newest')).toBe('newest');
		expect(readBrowseSort('updated')).toBe('updated');
		expect(readBrowseSort('name')).toBe('name');
	});

	it('falls back to the default for unknown and missing values', () => {
		expect(readBrowseSort(null)).toBe(DEFAULT_BROWSE_SORT);
		expect(readBrowseSort('')).toBe(DEFAULT_BROWSE_SORT);
		expect(readBrowseSort('price')).toBe(DEFAULT_BROWSE_SORT);
	});

	it('falls back rather than passing hostile input through', () => {
		expect(readBrowseSort('stars; drop table listings')).toBe(DEFAULT_BROWSE_SORT);
		expect(readBrowseSort('__proto__')).toBe(DEFAULT_BROWSE_SORT);
	});
});

describe('readBrowseOrder', () => {
	it('accepts both directions', () => {
		expect(readBrowseOrder('asc', 'stars')).toBe('asc');
		expect(readBrowseOrder('desc', 'stars')).toBe('desc');
	});

	it("falls back to the sort's own default direction", () => {
		expect(readBrowseOrder(null, 'stars')).toBe('desc');
		expect(readBrowseOrder(null, 'newest')).toBe('desc');
		expect(readBrowseOrder(null, 'updated')).toBe('desc');
		expect(readBrowseOrder(null, 'name')).toBe('asc');
	});

	it('rejects unknown directions', () => {
		expect(readBrowseOrder('sideways', 'name')).toBe('asc');
		expect(readBrowseOrder("'", 'stars')).toBe('desc');
	});
});

describe('readBrowseSorting', () => {
	it('reads sort and order together', () => {
		const params = new URLSearchParams({ sort: 'newest', order: 'asc' });
		expect(readBrowseSorting(params)).toEqual({ sort: 'newest', order: 'asc' });
	});

	it('defaults the order to the parsed sort, not the default sort', () => {
		const params = new URLSearchParams({ sort: 'name' });
		expect(readBrowseSorting(params)).toEqual({ sort: 'name', order: 'asc' });
	});

	it('returns the default pair for empty params', () => {
		expect(readBrowseSorting(new URLSearchParams())).toEqual({ sort: 'stars', order: 'desc' });
	});
});

describe('sort option tokens', () => {
	it('offers both directions for every sort', () => {
		for (const sort of ['stars', 'newest', 'updated', 'name'] as const) {
			const orders = BROWSE_SORT_OPTIONS.filter((option) => option.sort === sort).map(
				(option) => option.order
			);
			expect(orders.toSorted()).toEqual(['asc', 'desc']);
		}
	});

	it('uses unique values and labels', () => {
		const values = BROWSE_SORT_OPTIONS.map((option) => option.value);
		const labels = BROWSE_SORT_OPTIONS.map((option) => option.label);
		expect(new Set(values).size).toBe(values.length);
		expect(new Set(labels).size).toBe(labels.length);
	});

	it('round-trips every option through its token', () => {
		for (const option of BROWSE_SORT_OPTIONS) {
			expect(toSortValue(option)).toBe(option.value);
			expect(fromSortValue(option.value)).toEqual({ sort: option.sort, order: option.order });
		}
	});

	it('falls back to the default pair for an unknown token', () => {
		expect(fromSortValue('bogus')).toEqual({
			sort: DEFAULT_BROWSE_SORT,
			order: defaultOrderFor(DEFAULT_BROWSE_SORT)
		});
	});

	it('includes the default pair as a selectable option', () => {
		const token = toSortValue({
			sort: DEFAULT_BROWSE_SORT,
			order: defaultOrderFor(DEFAULT_BROWSE_SORT)
		});
		expect(BROWSE_SORT_OPTIONS.some((option) => option.value === token)).toBe(true);
	});
});

describe('toBrowseQueryString', () => {
	it('always includes the sort and order', () => {
		expect(toBrowseQueryString({ sort: 'stars', order: 'desc' })).toBe('sort=stars&order=desc');
		expect(toBrowseQueryString({ sort: 'name', order: 'asc' })).toBe('sort=name&order=asc');
	});

	it('appends a positive offset last', () => {
		expect(toBrowseQueryString({ sort: 'updated', order: 'desc' }, 24)).toBe(
			'sort=updated&order=desc&offset=24'
		);
	});

	it('omits a zero offset', () => {
		expect(toBrowseQueryString({ sort: 'stars', order: 'desc' }, 0)).toBe('sort=stars&order=desc');
	});

	it('round-trips through readBrowseSorting for every option', () => {
		for (const option of BROWSE_SORT_OPTIONS) {
			const sorting = { sort: option.sort, order: option.order };
			const params = new URLSearchParams(toBrowseQueryString(sorting));
			expect(readBrowseSorting(params)).toEqual(sorting);
		}
	});

	it('round-trips the offset through readBrowseOffset', () => {
		const params = new URLSearchParams(toBrowseQueryString({ sort: 'stars', order: 'desc' }, 48));
		expect(readBrowseOffset(params.get('offset'))).toBe(48);
	});
});

describe('isFeaturedRequested', () => {
	it('recognises the featured flag', () => {
		expect(isFeaturedRequested('true')).toBe(true);
	});

	it('treats a missing or falsy flag as a normal browse request', () => {
		expect(isFeaturedRequested(null)).toBe(false);
		expect(isFeaturedRequested('')).toBe(false);
		expect(isFeaturedRequested('false')).toBe(false);
	});

	it('does not accept other truthy-looking values', () => {
		expect(isFeaturedRequested('1')).toBe(false);
		expect(isFeaturedRequested('TRUE')).toBe(false);
		expect(isFeaturedRequested('yes')).toBe(false);
	});
});
