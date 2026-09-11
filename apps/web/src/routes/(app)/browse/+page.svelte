<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import {
		BrowseSort,
		ListingFeed,
		ListingToolbar,
		Section
	} from '$lib/components/storefront/index.ts';
	import { toBrowseQueryString } from '$lib/browse.ts';
	import type { ListingCategory } from '$lib/categories.ts';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();

	function selectCategory(category: ListingCategory | null): void {
		const query = toBrowseQueryString({ sort: data.sort, order: data.order }, 0, category);

		void goto(resolve(`/(app)/browse?${query}`), { keepFocus: true, noScroll: true });
	}
</script>

<svelte:head><title>Browse · Yuki</title></svelte:head>

{#snippet sortMenu()}
	<BrowseSort sort={data.sort} order={data.order} category={data.category} target="browse" />
{/snippet}

<main class="mx-auto w-full max-w-6xl space-y-8 px-4 py-8">
	<Section title="Apps" isHeaderSticky>
		{#snippet action()}
			<ListingToolbar
				categories={data.categories}
				selected={data.category}
				onSelect={selectCategory}
				sort={sortMenu}
			/>
		{/snippet}
		{#key `${data.sort}-${data.order}-${data.category ?? 'all'}`}
			<ListingFeed
				sort={data.sort}
				order={data.order}
				category={data.category}
				initialPage={data.browse}
			/>
		{/key}
	</Section>
</main>
