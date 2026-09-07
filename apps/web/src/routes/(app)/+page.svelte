<script lang="ts">
	import { resolve } from '$app/paths';
	import {
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
	<ProductCard
		title={entry.title}
		href={resolve('/(app)/listings/[slug]', { slug: entry.slug })}
		image={entry.iconUrl ? icon : undefined}
		{meta}
	/>
	{#snippet icon()}
		<img src={entry.iconUrl} alt="" class="size-full object-cover" loading="lazy" />
	{/snippet}
	{#snippet meta()}
		<p class="text-xs text-muted-foreground">{entry.author}</p>
	{/snippet}
{/snippet}

<main class="mx-auto w-full max-w-6xl space-y-12 px-4 py-8">
	<Section title="Featured">
		<ProductCarousel items={data.featured} item={card} />
	</Section>

	<Section title="New">
		<ProductGrid items={data.recent} item={card} />
	</Section>
</main>
