<script lang="ts">
	import type { ResolvedPathname } from '$app/types';
	import {
		ProductCard,
		ProductCarousel,
		ProductGrid,
		Section
	} from '$lib/components/storefront/index.ts';

	type StorefrontItem = { id: string; title: string; href: ResolvedPathname; badge?: string };

	const featured: StorefrontItem[] = [];
	const newArrivals: StorefrontItem[] = [];
	const browse: StorefrontItem[] = [];
</script>

{#snippet card(entry: StorefrontItem)}
	<ProductCard title={entry.title} href={entry.href} badge={entry.badge} />
{/snippet}

<main class="mx-auto w-full max-w-6xl space-y-12 px-4 py-8">
	<Section title="Featured">
		<ProductCarousel items={featured} item={card} />
	</Section>

	<Section title="New">
		<ProductGrid items={newArrivals} item={card} />
	</Section>

	<Section title="Browse">
		<ProductGrid items={browse} item={card} />
	</Section>
</main>
