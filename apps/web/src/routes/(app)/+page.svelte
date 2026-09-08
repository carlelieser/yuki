<script lang="ts">
	import { resolve } from '$app/paths';
	import { buttonVariants } from '@yuki/ui';
	import ArrowRightIcon from '@lucide/svelte/icons/arrow-right';
	import {
		BrowseSort,
		ListingCard,
		ListingFeed,
		ProductCard,
		ProductCarousel,
		Section
	} from '$lib/components/storefront/index.ts';
	import { toBrowseQueryString } from '$lib/browse.ts';
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
</script>

{#snippet card(entry: ListingSummary)}
	<ListingCard {entry} />
{/snippet}

{#snippet wideCard(entry: ListingSummary)}
	<ProductCard
		variant="wide"
		title={entry.title}
		href={resolve('/(app)/listings/[slug]', { slug: entry.slug })}
		image={entry.bannerUrl ? banner : undefined}
		icon={entry.iconUrl ? icon : undefined}
		{meta}
	/>
	{#snippet banner()}
		<img src={entry.bannerUrl} alt="" class="size-full object-cover" loading="lazy" />
	{/snippet}
	{#snippet icon()}
		<img src={entry.iconUrl} alt="" class="size-full object-cover" loading="lazy" />
	{/snippet}
	{#snippet meta()}
		<p class="truncate text-xs text-muted-foreground">{entry.author}</p>
	{/snippet}
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
