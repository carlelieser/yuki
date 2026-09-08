<script lang="ts">
	import { resolve } from '$app/paths';
	import ProductCard from './product-card.svelte';
	import type { ListingSummary } from '$lib/server/listings.ts';

	let { entry }: { entry: ListingSummary } = $props();
</script>

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
	<p class="truncate text-xs text-muted-foreground">{entry.author}</p>
{/snippet}
