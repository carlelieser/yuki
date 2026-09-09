<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { Button } from '@yuki/ui';
	import { toBrowseQueryString, type BrowseOrder, type BrowseSort } from '$lib/browse.ts';
	import type { CategoryOption, ListingCategory } from '$lib/categories.ts';

	let {
		categories,
		selected = null,
		sort,
		order
	}: {
		categories: CategoryOption[];
		selected?: ListingCategory | null;
		sort: BrowseSort;
		order: BrowseOrder;
	} = $props();

	function select(category: ListingCategory | null): void {
		const next = category === selected ? null : category;
		const query = toBrowseQueryString({ sort, order }, 0, next);

		void goto(resolve(`/(app)/browse?${query}`), { keepFocus: true, noScroll: true });
	}
</script>

<div class="-mx-4 overflow-x-auto px-4 pb-1">
	<div class="flex w-max gap-2" role="group" aria-label="Filter listings by category">
		<Button
			size="sm"
			variant={selected === null ? 'secondary' : 'outline'}
			aria-pressed={selected === null}
			onclick={() => select(null)}
		>
			All
		</Button>
		{#each categories as category (category.value)}
			<Button
				size="sm"
				variant={selected === category.value ? 'secondary' : 'outline'}
				aria-pressed={selected === category.value}
				onclick={() => select(category.value)}
			>
				{category.label}
			</Button>
		{/each}
	</div>
</div>
