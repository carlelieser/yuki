<script lang="ts">
	import CategoryMenu from './category-menu.svelte';
	import ViewModeToggle from './view-mode-toggle.svelte';
	import type { CategoryOption, ListingCategory } from '$lib/categories.ts';
	import type { Snippet } from 'svelte';

	let {
		categories,
		selected = null,
		onSelect,
		sort,
		action
	}: {
		categories: CategoryOption[];
		selected?: ListingCategory | null;
		onSelect: (category: ListingCategory | null) => void;
		sort: Snippet;
		action?: Snippet;
	} = $props();
</script>

<div class="flex items-center gap-1">
	<ViewModeToggle />
	<CategoryMenu {categories} {selected} {onSelect} />
	{@render sort()}
	{@render action?.()}
</div>
