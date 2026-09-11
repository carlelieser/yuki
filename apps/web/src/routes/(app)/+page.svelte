<script lang="ts">
	import { resolve } from '$app/paths';
	import { buttonVariants } from '@yuki/ui';
	import ArrowRightIcon from '@lucide/svelte/icons/arrow-right';
	import { goto } from '$app/navigation';
	import {
		BrowseSort,
		CategoryMenu,
		ListingCard,
		ListingFeed,
		ProductCarousel,
		Section
	} from '$lib/components/storefront/index.ts';
	import { toBrowseQueryString } from '$lib/browse.ts';
	import type { ListingCategory } from '$lib/categories.ts';
	import type { PageData } from './$types';
	import type { ListingSummary } from '$lib/server/listings.ts';
	import Autoplay from 'embla-carousel-autoplay';

	const FEATURED_AUTOPLAY_DELAY = 5000;

	let { data }: { data: PageData } = $props();

	const autoplay = Autoplay({
		delay: FEATURED_AUTOPLAY_DELAY,
		stopOnInteraction: false,
		stopOnMouseEnter: true
	});

	const browseHref = $derived(
		resolve(`/(app)/browse?${toBrowseQueryString({ sort: data.sort, order: data.order })}`)
	);

	function selectCategory(category: ListingCategory | null): void {
		const query = toBrowseQueryString({ sort: data.sort, order: data.order }, 0, category);

		void goto(resolve(`/(app)/browse?${query}`), { keepFocus: true, noScroll: true });
	}
</script>

<svelte:head><title>Yuki</title></svelte:head>

{#snippet card(entry: ListingSummary)}
	<ListingCard {entry} />
{/snippet}

{#snippet wideCard(entry: ListingSummary)}
	<ListingCard {entry} variant="wide" />
{/snippet}

<main class="mx-auto w-full max-w-6xl space-y-12 px-4 py-8">
	<Section title="Featured">
		<ProductCarousel
			items={data.featured}
			item={wideCard}
			variant="wide"
			itemClass="basis-full ps-2"
			opts={{ loop: true }}
			plugins={[autoplay]}
			hasDots
		/>
	</Section>

	<Section title="New">
		<ProductCarousel items={data.recent} item={card} />
	</Section>

	<Section title="Apps" isHeaderSticky>
		{#snippet action()}
			<div class="flex items-center gap-1">
				<CategoryMenu categories={data.categories} onSelect={selectCategory} />
				<BrowseSort sort={data.sort} order={data.order} />
				<a href={browseHref} class={buttonVariants({ variant: 'ghost' })}>
					Browse all
					<ArrowRightIcon aria-hidden="true" />
				</a>
			</div>
		{/snippet}
		{#key `${data.sort}-${data.order}`}
			<ListingFeed
				sort={data.sort}
				order={data.order}
				initialPage={data.browse}
				hasInfiniteScroll={false}
			/>
		{/key}
	</Section>
</main>
