<script lang="ts" generics="Item">
	import {
		Carousel,
		CarouselContent,
		CarouselDots,
		CarouselItem,
		CarouselNext,
		CarouselPrevious
	} from '@yuki/ui';
	import CollectionEmpty from './collection-empty.svelte';
	import ProductCardSkeleton from './product-card-skeleton.svelte';
	import type { CarouselAPI, CarouselOptions, CarouselPlugins } from '@yuki/ui';
	import type { Snippet } from 'svelte';

	let {
		items,
		item,
		isLoading = false,
		skeletonCount = 4,
		empty,
		variant = 'default',
		itemClass = 'basis-1/2 sm:basis-1/3 lg:basis-1/4 ps-2',
		opts,
		plugins,
		hasDots = false
	}: {
		items: Item[];
		item: Snippet<[Item]>;
		isLoading?: boolean;
		skeletonCount?: number;
		empty?: Snippet;
		variant?: 'default' | 'wide';
		itemClass?: string;
		opts?: CarouselOptions;
		plugins?: CarouselPlugins;
		hasDots?: boolean;
	} = $props();

	function restartAutoplayOnManualScroll(api: CarouselAPI | undefined): void {
		if (api === undefined) return;

		let isAutoplayScroll = false;

		api
			.on('autoplay:select', () => {
				isAutoplayScroll = true;
			})
			.on('select', () => {
				const autoplay = api.plugins().autoplay;
				if (autoplay === undefined) return;

				if (!isAutoplayScroll && autoplay.isPlaying()) autoplay.reset();
				isAutoplayScroll = false;
			});
	}
</script>

{#if !isLoading && items.length === 0}
	{#if empty}
		{@render empty()}
	{:else}
		<CollectionEmpty />
	{/if}
{:else}
	<Carousel
		opts={{ align: 'start', ...opts }}
		{plugins}
		setApi={restartAutoplayOnManualScroll}
		class="-ml-1"
	>
		<CarouselContent class="-ms-2">
			{#if isLoading}
				{#each { length: skeletonCount }, index (index)}
					<CarouselItem class={itemClass}>
						<div class="p-1"><ProductCardSkeleton {variant} /></div>
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
		{#if hasDots}
			<CarouselDots class="mt-4" />
		{/if}
	</Carousel>
{/if}
