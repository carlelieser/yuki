<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import SortMenu from './sort-menu.svelte';
	import {
		fromSearchSortValue,
		SEARCH_SORT_OPTIONS,
		toSearchQueryString,
		toSearchSortValue,
		type SearchSorting
	} from '$lib/search-query.ts';
	import type { ListingCategory } from '$lib/categories.ts';

	let {
		query,
		sorting,
		category = null
	}: { query: string; sorting: SearchSorting; category?: ListingCategory | null } = $props();

	const current = $derived(toSearchSortValue(sorting));

	function select(value: string): void {
		const selected = fromSearchSortValue(value);
		void goto(resolve(`/(app)/search?${toSearchQueryString(query, selected, 0, category)}`), {
			keepFocus: true,
			noScroll: true
		});
	}
</script>

<SortMenu options={SEARCH_SORT_OPTIONS} value={current} onSelect={select} />
