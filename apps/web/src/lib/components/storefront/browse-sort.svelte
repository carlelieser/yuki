<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import SortMenu from './sort-menu.svelte';
	import {
		BROWSE_SORT_OPTIONS,
		fromSortValue,
		toSortValue,
		type BrowseOrder,
		type BrowseSort
	} from '$lib/browse.ts';

	let { sort, order }: { sort: BrowseSort; order: BrowseOrder } = $props();

	const current = $derived(toSortValue({ sort, order }));

	function select(value: string): void {
		const selected = fromSortValue(value);
		void goto(resolve(`/?sort=${selected.sort}&order=${selected.order}`), {
			keepFocus: true,
			noScroll: true
		});
	}
</script>

<SortMenu options={BROWSE_SORT_OPTIONS} value={current} onSelect={select} />
