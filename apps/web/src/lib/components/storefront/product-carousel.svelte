<script lang="ts" generics="Item">
	import {
		Carousel,
		CarouselContent,
		CarouselItem,
		CarouselNext,
		CarouselPrevious
	} from '@yuki/ui';
	import ProductCardSkeleton from './product-card-skeleton.svelte';
	import type { Snippet } from 'svelte';

	let {
		items,
		item,
		isLoading = false,
		skeletonCount = 4
	}: {
		items: Item[];
		item: Snippet<[Item]>;
		isLoading?: boolean;
		skeletonCount?: number;
	} = $props();

	const itemClass = 'basis-1/2 sm:basis-1/3 lg:basis-1/4';
</script>

<Carousel opts={{ align: 'start' }}>
	<CarouselContent class="py-1">
		{#if isLoading}
			{#each { length: skeletonCount }, index (index)}
				<CarouselItem class={itemClass}><ProductCardSkeleton /></CarouselItem>
			{/each}
		{:else}
			{#each items as entry, index (index)}
				<CarouselItem class={itemClass}>{@render item(entry)}</CarouselItem>
			{/each}
		{/if}
	</CarouselContent>
	<CarouselPrevious class="hidden sm:flex" />
	<CarouselNext class="hidden sm:flex" />
</Carousel>
