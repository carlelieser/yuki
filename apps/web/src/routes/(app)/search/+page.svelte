<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import {
		CollectionEmpty,
		ListingCard,
		ListingToolbar,
		ProductGrid,
		SearchSort,
		Section
	} from '$lib/components/storefront/index.ts';
	import { toSearchQueryString } from '$lib/search-query.ts';
	import type { ListingCategory } from '$lib/categories.ts';
	import type { PageData } from './$types';
	import type { ListingSummary } from '$lib/server/listings.ts';

	let { data }: { data: PageData } = $props();

	const title = $derived(data.query === '' ? 'Search · Yuki' : `${data.query} · Yuki`);

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

<svelte:head><title>{title}</title></svelte:head>

{#snippet card(entry: ListingSummary)}
	<ListingCard {entry} />
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
				<a
					href={nextPageHref}
					class="text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
				>
					Load more
				</a>
			</div>
		{/if}
	{/if}
</main>
