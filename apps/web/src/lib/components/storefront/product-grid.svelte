<script lang="ts" generics="Item">
	import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@yuki/ui';
	import PackageOpenIcon from '@lucide/svelte/icons/package-open';
	import ProductCardSkeleton from './product-card-skeleton.svelte';
	import type { Snippet } from 'svelte';

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
</script>

{#if isLoading}
	<ul class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
		{#each { length: skeletonCount }, index (index)}
			<li><ProductCardSkeleton /></li>
		{/each}
	</ul>
{:else if items.length === 0}
	{#if empty}
		{@render empty()}
	{:else}
		<Empty class="border">
			<EmptyHeader>
				<EmptyMedia variant="icon"><PackageOpenIcon /></EmptyMedia>
				<EmptyTitle>Nothing here yet</EmptyTitle>
				<EmptyDescription>Check back soon for new arrivals.</EmptyDescription>
			</EmptyHeader>
		</Empty>
	{/if}
{:else}
	<ul class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
		{#each items as entry, index (index)}
			<li>{@render item(entry)}</li>
		{/each}
	</ul>
{/if}
