<script lang="ts">
	import { Number, Progress, StarRating, Item } from '@yuki/ui';
	import type { RatingSummary } from '$lib/server/reviews.ts';

	let { summary }: { summary: RatingSummary } = $props();

	function share(count: number): number {
		return summary.total === 0 ? 0 : (count / summary.total) * 100;
	}
</script>

<Item class="items-end">
	<div class="flex shrink-0 flex-col gap-1 sm:w-32">
		<span class="text-6xl font-semibold tracking-tight">
			<Number value={summary.average} maximumFractionDigits={1} />
		</span>
		<StarRating value={summary.average} />
		<span class="text-sm text-muted-foreground">
			<Number value={summary.total} preset="compact" />
			{summary.total === 1 ? 'review' : 'reviews'}
		</span>
	</div>

	<div class="w-full flex-1 space-y-1.5">
		{#each summary.distribution as bucket (bucket.rating)}
			<div class="flex items-center gap-3">
				<span class="w-3 shrink-0 text-sm tabular-nums text-muted-foreground">{bucket.rating}</span>
				<Progress value={share(bucket.count)} class="flex-1" aria-hidden="true" tabindex={-1} />
				<span class="w-10 shrink-0 text-end text-sm tabular-nums text-muted-foreground">
					{bucket.count}
				</span>
			</div>
		{/each}
	</div>
</Item>
