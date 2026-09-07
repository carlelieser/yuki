<script lang="ts" generics="Item">
	import {
		Carousel,
		CarouselContent,
		CarouselItem,
		CarouselNext,
		CarouselPrevious
	} from '@yuki/ui';
	import CollectionEmpty from './collection-empty.svelte';
	import ProductCardSkeleton from './product-card-skeleton.svelte';
	import type { Snippet } from 'svelte';

	let {
		items,
		item,
		isLoading = false,
		skeletonCount = 4,
		empty
	}: {
		items: Item[];
		item: Snippet<[Item]>;
		isLoading?: boolean;
		skeletonCount?: number;
		empty?: Snippet;
	} = $props();

	const itemClass = 'basis-1/2 sm:basis-1/3 lg:basis-1/4 ps-2';
</script>

{#if !isLoading && items.length === 0}
	{#if empty}
		{@render empty()}
	{:else}
		<CollectionEmpty />
	{/if}
{:else}
	<Carousel opts={{ align: 'start' }} class="-ml-1">
		<CarouselContent class="-ms-2">
			{#if isLoading}
				{#each { length: skeletonCount }, index (index)}
					<CarouselItem class={itemClass}>
						<div class="p-1"><ProductCardSkeleton /></div>
					</CarouselItem>
				{/each}
			{:else}
				{#each items as entry, index (index)}
					<CarouselItem class={itemClass}>
						<div class="p-1">{@render item(entry)}</div>
					</CarouselItem>
				{/each}
			{/if}
		</CarouselContent>
		<CarouselPrevious class="start-6 hidden disabled:invisible sm:flex" />
		<CarouselNext class="end-6 hidden disabled:invisible sm:flex" />
	</Carousel>
{/if}
