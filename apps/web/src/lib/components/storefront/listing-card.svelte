<script lang="ts">
	import { resolve } from '$app/paths';
	import ProductCard from './product-card.svelte';
	import type { ListingSummary } from '$lib/server/listings.ts';

	let { entry, variant = 'default' }: { entry: ListingSummary; variant?: 'default' | 'wide' } =
		$props();

	const media = $derived(variant === 'wide' ? entry.bannerUrl : entry.iconUrl);
</script>

<ProductCard
	{variant}
	title={entry.title}
	href={resolve('/(app)/listings/[slug]', { slug: entry.slug })}
	image={media ? image : undefined}
	icon={entry.iconUrl ? icon : undefined}
	{meta}
/>

{#snippet image()}
	<img src={media} alt="" class="size-full object-cover" loading="lazy" />
{/snippet}

{#snippet icon()}
	<img src={entry.iconUrl} alt="" class="size-full object-cover" loading="lazy" />
{/snippet}

{#snippet meta()}
	<p class="truncate text-xs text-muted-foreground">{entry.author}</p>
{/snippet}
