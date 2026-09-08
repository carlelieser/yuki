<script lang="ts">
	import { resolve } from '$app/paths';
	import {
		ListingCard,
		ListingFeed,
		ProductCard,
		ProductCarousel,
		ProductGrid,
		Section
	} from '$lib/components/storefront/index.ts';
	import type { PageData } from './$types';
	import type { ListingSummary } from '$lib/server/listings.ts';

	let { data }: { data: PageData } = $props();
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
			itemClass="basis-4/5 sm:basis-1/2 lg:basis-1/3 ps-2"
		/>
	</Section>

	<Section title="New">
		<ProductGrid items={data.recent} item={card} />
	</Section>

	<Section title="Browse">
		<ListingFeed />
	</Section>
</main>
