<script lang="ts">
	import {
		BrowseSort,
		CategoryFilter,
		ListingFeed,
		Section
	} from '$lib/components/storefront/index.ts';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();
</script>

<svelte:head><title>Browse · Yuki</title></svelte:head>

<main class="mx-auto w-full max-w-6xl space-y-8 px-4 py-8">
	<Section title="Apps" isHeaderSticky>
		{#snippet action()}
			<BrowseSort sort={data.sort} order={data.order} target="browse" />
		{/snippet}
		<CategoryFilter
			categories={data.categories}
			selected={data.category}
			sort={data.sort}
			order={data.order}
		/>
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
