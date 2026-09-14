<script lang="ts">
	import { resolve } from '$app/paths';
	import ListingBadges from './listing-badges.svelte';
	import ProductCard, { type ProductCardVariant } from './product-card.svelte';
	import type { ListingSummary } from '$lib/server/listings.ts';

	let {
		entry,
		variant = 'default'
	}: { entry: ListingSummary; variant?: Exclude<ProductCardVariant, 'detail'> } = $props();

	const media = $derived(variant === 'wide' ? entry.bannerUrl : entry.iconUrl);
</script>

<ProductCard
	{variant}
	title={entry.title}
	description={entry.description}
	href={resolve('/(app)/listings/[slug]', { slug: entry.slug })}
	image={media ? image : undefined}
	icon={entry.iconUrl ? icon : undefined}
	{badges}
/>

{#snippet image()}
	<img
		src={media}
		alt=""
		class="size-full object-cover"
		loading="lazy"
		decoding="async"
		referrerpolicy="no-referrer"
	/>
{/snippet}

{#snippet icon()}
	<img
		src={entry.iconUrl}
		alt=""
		class="size-full object-cover"
		loading="lazy"
		decoding="async"
		referrerpolicy="no-referrer"
	/>
{/snippet}

{#snippet badges()}
	<ListingBadges {entry} />
{/snippet}
