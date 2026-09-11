<script lang="ts" generics="Item">
	import CollectionEmpty from './collection-empty.svelte';
	import ProductCardSkeleton from './product-card-skeleton.svelte';
	import { cardVariantFor } from './product-card.svelte';
	import { getViewModeStore } from '$lib/view-mode-store.svelte.ts';
	import type { Snippet } from 'svelte';

	const LAYOUTS = {
		grid: 'grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4',
		list: 'flex flex-col gap-3'
	} as const;

	let {
		items,
		item,
		isLoading = false,
		skeletonCount = 8,
		empty
	}: {
		items: Item[];
		item: Snippet<[Item]>;
		isLoading?: boolean;
		skeletonCount?: number;
		empty?: Snippet;
	} = $props();

	const views = getViewModeStore();
	const layout = $derived(LAYOUTS[views.mode]);
</script>

{#if isLoading}
	<ul role="list" class={layout}>
		{#each { length: skeletonCount }, index (index)}
			<li><ProductCardSkeleton variant={cardVariantFor(views.mode)} /></li>
		{/each}
	</ul>
{:else if items.length === 0}
	{#if empty}
		{@render empty()}
	{:else}
		<CollectionEmpty />
	{/if}
{:else}
	<ul role="list" class={layout}>
		{#each items as entry, index (index)}
			<li>{@render item(entry)}</li>
		{/each}
	</ul>
{/if}
