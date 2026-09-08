export const BROWSE_PAGE_SIZE = 24;
export const MAX_BROWSE_OFFSET = 2000;

export const BROWSE_SORTS = ['stars', 'newest', 'updated', 'name'] as const;
export const BROWSE_ORDERS = ['asc', 'desc'] as const;

export type BrowseSort = (typeof BROWSE_SORTS)[number];
export type BrowseOrder = (typeof BROWSE_ORDERS)[number];

export type BrowseSorting = { sort: BrowseSort; order: BrowseOrder };

export const DEFAULT_BROWSE_SORT: BrowseSort = 'stars';

const DEFAULT_ORDERS: Record<BrowseSort, BrowseOrder> = {
	stars: 'desc',
	newest: 'desc',
	updated: 'desc',
	name: 'asc'
};

export type BrowseSortOption = BrowseSorting & { value: string; label: string };

export const BROWSE_SORT_OPTIONS: BrowseSortOption[] = [
	{ value: 'stars-desc', label: 'Most stars', sort: 'stars', order: 'desc' },
	{ value: 'stars-asc', label: 'Fewest stars', sort: 'stars', order: 'asc' },
	{ value: 'newest-desc', label: 'Newest first', sort: 'newest', order: 'desc' },
	{ value: 'newest-asc', label: 'Oldest first', sort: 'newest', order: 'asc' },
	{ value: 'updated-desc', label: 'Recently updated', sort: 'updated', order: 'desc' },
	{ value: 'updated-asc', label: 'Least recently updated', sort: 'updated', order: 'asc' },
	{ value: 'name-asc', label: 'Name A–Z', sort: 'name', order: 'asc' },
	{ value: 'name-desc', label: 'Name Z–A', sort: 'name', order: 'desc' }
];

export function readBrowseOffset(raw: string | null): number {
	const parsed = Number(raw);
	if (!Number.isFinite(parsed) || parsed <= 0) return 0;
	return Math.min(Math.floor(parsed), MAX_BROWSE_OFFSET);
}

export function defaultOrderFor(sort: BrowseSort): BrowseOrder {
	return DEFAULT_ORDERS[sort];
}

export function readBrowseSort(raw: string | null): BrowseSort {
	return BROWSE_SORTS.find((sort) => sort === raw) ?? DEFAULT_BROWSE_SORT;
}

export function readBrowseOrder(raw: string | null, sort: BrowseSort): BrowseOrder {
	return BROWSE_ORDERS.find((order) => order === raw) ?? defaultOrderFor(sort);
}

export function readBrowseSorting(params: URLSearchParams): BrowseSorting {
	const sort = readBrowseSort(params.get('sort'));
	return { sort, order: readBrowseOrder(params.get('order'), sort) };
}

export function toSortValue(sorting: BrowseSorting): string {
	return `${sorting.sort}-${sorting.order}`;
}

export function fromSortValue(value: string): BrowseSorting {
	const option = BROWSE_SORT_OPTIONS.find((entry) => entry.value === value);
	if (option === undefined) {
		return { sort: DEFAULT_BROWSE_SORT, order: defaultOrderFor(DEFAULT_BROWSE_SORT) };
	}
	return { sort: option.sort, order: option.order };
}

export function toBrowseQueryString(sorting: BrowseSorting, offset: number = 0): string {
	const parts = [`sort=${sorting.sort}`, `order=${sorting.order}`];
	if (offset > 0) parts.push(`offset=${offset}`);
	return parts.join('&');
}

export const REVIEWS_PAGE_SIZE = 10;
