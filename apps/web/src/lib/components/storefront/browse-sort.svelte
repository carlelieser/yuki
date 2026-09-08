<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import SortMenu from './sort-menu.svelte';
	import {
		BROWSE_SORT_OPTIONS,
		fromSortValue,
		toBrowseQueryString,
		toSortValue,
		type BrowseOrder,
		type BrowseSort
	} from '$lib/browse.ts';

	let {
		sort,
		order,
		target = 'home'
	}: { sort: BrowseSort; order: BrowseOrder; target?: 'home' | 'browse' } = $props();

	const current = $derived(toSortValue({ sort, order }));

	function select(value: string): void {
		const query = toBrowseQueryString(fromSortValue(value));
		const href = target === 'browse' ? resolve(`/(app)/browse?${query}`) : resolve(`/?${query}`);

		void goto(href, { keepFocus: true, noScroll: true });
	}
</script>

<SortMenu options={BROWSE_SORT_OPTIONS} value={current} onSelect={select} />
