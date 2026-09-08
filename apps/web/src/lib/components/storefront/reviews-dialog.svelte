<script lang="ts">
	import { resolve } from '$app/paths';
	import {
		Dialog,
		DialogContent,
		DialogDescription,
		DialogHeader,
		DialogTitle,
		ScrollArea,
		Skeleton
	} from '@yuki/ui';
	import ReviewCard from './review-card.svelte';
	import type { Review, ReviewPage } from '$lib/server/reviews.ts';

	let {
		slug,
		initial,
		total,
		open = $bindable(false)
	}: {
		slug: string;
		initial: Review[];
		total: number;
		open?: boolean;
	} = $props();

	let results = $state<Review[]>([]);
	let hasMore = $state(true);
	let isLoading = $state(false);
	let sentinel = $state<HTMLDivElement | null>(null);
	let viewport = $state<HTMLElement | null>(null);

	$effect(() => {
		if (!open) return;

		results = [...initial];
		hasMore = true;
	});

	async function loadMore(): Promise<void> {
		if (isLoading || !hasMore) return;
		isLoading = true;

		try {
			const endpoint = resolve('/api/listings/[slug]/reviews', { slug });
			const response = await fetch(`${endpoint}?offset=${results.length}`);

			if (!response.ok) {
				hasMore = false;
				return;
			}

			const page: ReviewPage = await response.json();
			results = [...results, ...page.results];
			hasMore = page.hasMore;
		} catch {
			hasMore = false;
		} finally {
			isLoading = false;
		}
	}

	$effect(() => {
		if (!open || sentinel === null || viewport === null) return;

		const observer = new IntersectionObserver(
			(entries) => {
				if (entries.some((entry) => entry.isIntersecting)) void loadMore();
			},
			{ root: viewport, rootMargin: '200px' }
		);

		observer.observe(sentinel);
		return () => observer.disconnect();
	});
</script>

<Dialog bind:open>
	<DialogContent class="max-w-2xl">
		<DialogHeader>
			<DialogTitle>All reviews</DialogTitle>
			<DialogDescription>
				{total}
				{total === 1 ? 'review' : 'reviews'}
			</DialogDescription>
		</DialogHeader>

		<ScrollArea bind:viewportRef={viewport} class="h-[60vh] pe-4">
			<div class="space-y-3">
				{#each results as review (review.id)}
					<ReviewCard {review} />
				{/each}

				{#if isLoading}
					<Skeleton class="h-20 w-full rounded-xl" />
				{/if}

				{#if hasMore}
					<div bind:this={sentinel} aria-hidden="true" class="h-8"></div>
				{/if}
			</div>
		</ScrollArea>
	</DialogContent>
</Dialog>
