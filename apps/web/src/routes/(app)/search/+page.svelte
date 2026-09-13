<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { Link } from '@yuki/ui';
	import {
		CollectionEmpty,
		ListingCard,
		ListingToolbar,
		ProductGrid,
		SearchSort,
		Section
	} from '$lib/components/storefront/index.ts';
	import { toSearchQueryString } from '$lib/search-query.ts';
	import { SeoHead } from '$lib/components/seo/index.ts';
	import { searchMeta } from '$lib/seo/page-meta.ts';
	import { getViewModeStore } from '$lib/view-mode-store.svelte.ts';
	import { cardVariantFor } from '$lib/components/storefront/product-card.svelte';
	import type { ListingCategory } from '$lib/categories.ts';
	import type { PageData } from './$types';
	import type { ListingSummary } from '$lib/server/listings.ts';

	let { data }: { data: PageData } = $props();

	const views = getViewModeStore();

	const heading = $derived(
		`${data.total} ${data.total === 1 ? 'result' : 'results'} for “${data.query}”`
	);
	const nextOffset = $derived(data.results.length);
	const nextPageHref = $derived(
		resolve(
			`/(app)/search?${toSearchQueryString(data.query, data.sorting, nextOffset, data.category)}`
		)
	);

	function selectCategory(category: ListingCategory | null): void {
		const query = toSearchQueryString(data.query, data.sorting, 0, category);

		void goto(resolve(`/(app)/search?${query}`), { keepFocus: true, noScroll: true });
	}
</script>

<SeoHead {...searchMeta(data.query, data.total)} />

{#snippet card(entry: ListingSummary)}
	<ListingCard {entry} variant={cardVariantFor(views.mode)} />
{/snippet}

{#snippet sortMenu()}
	<SearchSort query={data.query} sorting={data.sorting} category={data.category} />
{/snippet}

<main class="mx-auto w-full max-w-6xl space-y-8 px-4 py-8">
	{#if data.query === ''}
		<CollectionEmpty
			title="Search listings"
			description="Type a name, author, or keyword to find a listing."
		/>
	{:else}
		<h1 class="sr-only">{heading}</h1>

		<Section title={heading}>
			{#snippet action()}
				<ListingToolbar
					categories={data.categories}
					selected={data.category}
					onSelect={selectCategory}
					sort={sortMenu}
				/>
			{/snippet}
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
				<Link href={nextPageHref} class="text-muted-foreground">Load more</Link>
			</div>
		{/if}
	{/if}
</main>
