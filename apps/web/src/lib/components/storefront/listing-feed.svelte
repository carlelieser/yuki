<script lang="ts">
	import { resolve } from '$app/paths';
	import CollectionEmpty from './collection-empty.svelte';
	import ListingCard from './listing-card.svelte';
	import ProductGrid from './product-grid.svelte';
	import type { ListingPage, ListingSummary } from '$lib/server/listings.ts';

	let { skeletonCount = 8 }: { skeletonCount?: number } = $props();

	let results = $state<ListingSummary[]>([]);
	let hasMore = $state(true);
	let isLoading = $state(false);
	let hasLoadedOnce = $state(false);
	let sentinel = $state<HTMLDivElement | null>(null);

	async function loadMore(): Promise<void> {
		if (isLoading || !hasMore) return;
		isLoading = true;

		try {
			const response = await fetch(`${resolve('/api/listings')}?offset=${results.length}`);
			if (!response.ok) {
				hasMore = false;
				return;
			}

			const page: ListingPage = await response.json();
			results = [...results, ...page.results];
			hasMore = page.hasMore;
		} catch {
			hasMore = false;
		} finally {
			isLoading = false;
			hasLoadedOnce = true;
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
			<CollectionEmpty title="No listings yet" description="Check back soon for new arrivals." />
		{/snippet}
	</ProductGrid>

	{#if hasMore}
		<div bind:this={sentinel} aria-hidden="true" class="h-px"></div>
	{/if}
</div>
