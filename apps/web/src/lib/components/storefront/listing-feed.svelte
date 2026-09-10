<script lang="ts">
	import { resolve } from '$app/paths';
	import CollectionEmpty from './collection-empty.svelte';
	import ListingCard from './listing-card.svelte';
	import ProductGrid from './product-grid.svelte';
	import type { ListingPage, ListingSummary } from '$lib/server/listings.ts';
	import { toBrowseQueryString, type BrowseOrder, type BrowseSort } from '$lib/browse.ts';
	import type { ListingCategory } from '$lib/categories.ts';

	let {
		sort,
		order,
		category = null,
		initialPage,
		skeletonCount = 8,
		hasInfiniteScroll = true
	}: {
		sort: BrowseSort;
		order: BrowseOrder;
		category?: ListingCategory | null;
		initialPage?: ListingPage;
		skeletonCount?: number;
		hasInfiniteScroll?: boolean;
	} = $props();

	// svelte-ignore state_referenced_locally
	const seed = initialPage;

	let results = $state<ListingSummary[]>([...(seed?.results ?? [])]);
	let hasMore = $state(seed?.hasMore ?? true);
	let isLoading = $state(false);
	let hasLoadedOnce = $state(seed !== undefined);
	let sentinel = $state<HTMLDivElement | null>(null);

	let requestId = 0;

	async function loadMore(): Promise<void> {
		if (!hasInfiniteScroll || isLoading || !hasMore) return;
		isLoading = true;

		const currentRequest = ++requestId;
		const query = toBrowseQueryString({ sort, order }, results.length, category);

		try {
			const response = await fetch(`${resolve('/api/listings')}?${query}`);
			if (currentRequest !== requestId) return;

			if (!response.ok) {
				hasMore = false;
				return;
			}

			const page: ListingPage = await response.json();
			if (currentRequest !== requestId) return;

			results = [...results, ...page.results];
			hasMore = page.hasMore;
		} catch {
			if (currentRequest === requestId) hasMore = false;
		} finally {
			if (currentRequest === requestId) {
				isLoading = false;
				hasLoadedOnce = true;
			}
		}
	}

	$effect(() => {
		if (sentinel === null) return;

		const observer = new IntersectionObserver((entries) => {
			if (entries.some((entry) => entry.isIntersecting)) void loadMore();
		});

		observer.observe(sentinel);
		return () => observer.disconnect();
	});
</script>

{#snippet card(entry: ListingSummary)}
	<ListingCard {entry} />
{/snippet}

<div class="space-y-4">
	<ProductGrid items={results} item={card} isLoading={!hasLoadedOnce} {skeletonCount}>
		{#snippet empty()}
			<CollectionEmpty
				title="No listings here yet"
				description="Nothing matches this category right now. Try another one."
			/>
		{/snippet}
	</ProductGrid>

	{#if hasInfiniteScroll && hasMore}
		<div bind:this={sentinel} aria-hidden="true" class="h-px"></div>
	{/if}
</div>
