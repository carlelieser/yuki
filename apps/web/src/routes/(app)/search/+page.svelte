<script lang="ts">
	import { resolve } from '$app/paths';
	import {
		CollectionEmpty,
		ProductCard,
		ProductGrid,
		Section
	} from '$lib/components/storefront/index.ts';
	import type { PageData } from './$types';
	import type { ListingSummary } from '$lib/server/listings.ts';

	let { data }: { data: PageData } = $props();

	const heading = $derived(
		`${data.total} ${data.total === 1 ? 'result' : 'results'} for “${data.query}”`
	);
	const nextOffset = $derived(data.results.length);
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

<main class="mx-auto w-full max-w-6xl space-y-8 px-4 py-8">
	{#if data.query === ''}
		<CollectionEmpty
			title="Search listings"
			description="Type a name, author, or keyword to find a listing."
		/>
	{:else}
		<Section title={heading}>
			<ProductGrid items={data.results} item={card}>
				{#snippet empty()}
					<CollectionEmpty
						title="No results for “{data.query}”"
						description="Try a different spelling or a broader keyword."
					/>
				{/snippet}
			</ProductGrid>
		</Section>

		{#if data.hasMore}
			<div class="flex justify-center">
				<a
					href="{resolve('/(app)/search')}?q={encodeURIComponent(data.query)}&offset={nextOffset}"
					class="text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
				>
					Load more
				</a>
			</div>
		{/if}
	{/if}
</main>
