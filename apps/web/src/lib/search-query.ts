import {
	BROWSE_SORT_OPTIONS,
	BROWSE_SORTS,
	readBrowseOrder,
	toSortValue,
	type BrowseOrder,
	type BrowseSort
} from './browse.ts';

export const MAX_QUERY_LENGTH = 100;
export const MIN_TRIGRAM_LENGTH = 3;
export const MAX_SEARCH_OFFSET = 500;
export const SEARCH_PAGE_SIZE = 8;
export const MAX_SEARCH_PAGE_SIZE = 24;

const TOKEN_ALLOWED_CHARACTERS = /[^A-Za-z0-9._-]/g;

export function normalizeSearchQuery(raw: unknown): string {
	if (typeof raw !== 'string') return '';
	return raw.replace(/\s+/g, ' ').trim().slice(0, MAX_QUERY_LENGTH);
}

export function toPrefixTsQuery(normalized: string): string {
	const tokens = normalized
		.split(' ')
		.map((token) => token.replace(TOKEN_ALLOWED_CHARACTERS, ''))
		.filter((token) => token.length > 0);

	if (tokens.length === 0) return '';

	return tokens
		.map((token, index) => (index === tokens.length - 1 ? `${token}:*` : token))
		.join(' & ');
}

export function hasEnoughLengthForTrigram(normalized: string): boolean {
	return normalized.length >= MIN_TRIGRAM_LENGTH;
}

export function readSearchLimit(raw: string | null): number {
	const parsed = Number(raw);
	if (!Number.isFinite(parsed) || parsed <= 0) return SEARCH_PAGE_SIZE;
	return Math.min(Math.floor(parsed), MAX_SEARCH_PAGE_SIZE);
}

export function readOffset(raw: string | null): number {
	const parsed = Number(raw);
	if (!Number.isFinite(parsed) || parsed <= 0) return 0;
	return Math.min(Math.floor(parsed), MAX_SEARCH_OFFSET);
}

export const SEARCH_RELEVANCE_SORT = 'relevance';

export type SearchSort = BrowseSort | typeof SEARCH_RELEVANCE_SORT;
export type SearchSorting = { sort: SearchSort; order: BrowseOrder };

export const DEFAULT_SEARCH_SORTING: SearchSorting = {
	sort: SEARCH_RELEVANCE_SORT,
	order: 'desc'
};

export type SearchSortOption = SearchSorting & { value: string; label: string };

export const SEARCH_SORT_OPTIONS: SearchSortOption[] = [
	{
		value: SEARCH_RELEVANCE_SORT,
		label: 'Best match',
		sort: SEARCH_RELEVANCE_SORT,
		order: 'desc'
	},
	...BROWSE_SORT_OPTIONS
];

export function isRelevanceSort(sort: SearchSort): sort is typeof SEARCH_RELEVANCE_SORT {
	return sort === SEARCH_RELEVANCE_SORT;
}

export function toSearchSortValue(sorting: SearchSorting): string {
	if (isRelevanceSort(sorting.sort)) return SEARCH_RELEVANCE_SORT;
	return toSortValue({ sort: sorting.sort, order: sorting.order });
}

export function fromSearchSortValue(value: string): SearchSorting {
	const option = SEARCH_SORT_OPTIONS.find((entry) => entry.value === value);
	if (option === undefined) return DEFAULT_SEARCH_SORTING;
	return { sort: option.sort, order: option.order };
}

export function readSearchSorting(params: URLSearchParams): SearchSorting {
	const raw = params.get('sort');
	if (raw === null || raw === SEARCH_RELEVANCE_SORT) return DEFAULT_SEARCH_SORTING;

	const sort = BROWSE_SORTS.find((entry) => entry === raw);
	if (sort === undefined) return DEFAULT_SEARCH_SORTING;

	return { sort, order: readBrowseOrder(params.get('order'), sort) };
}

export function toSearchQueryString(
	query: string,
	sorting: SearchSorting,
	offset: number = 0,
	category: string | null = null
): string {
	const parts = [`q=${encodeURIComponent(query)}`];

	if (!isRelevanceSort(sorting.sort)) {
		parts.push(`sort=${sorting.sort}`, `order=${sorting.order}`);
	}

	if (offset > 0) parts.push(`offset=${offset}`);
	if (category !== null) parts.push(`category=${category}`);

	return parts.join('&');
}
