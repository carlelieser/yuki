<script lang="ts">
	import { Badge, Number } from '@yuki/ui';
	import StarIcon from '@lucide/svelte/icons/star';
	import ThumbsUpIcon from '@lucide/svelte/icons/thumbs-up';
	import UserIcon from '@lucide/svelte/icons/user';
	import { badgesFor } from './listing-badges.ts';
	import { categoryIcon } from '$lib/category-icons.ts';
	import type { ListingSummary } from '$lib/server/listings.ts';

	let { entry }: { entry: ListingSummary } = $props();

	const badges = $derived(badgesFor(entry));
</script>

<div class="flex flex-wrap items-center gap-1">
	{#each badges as badge (badge.kind)}
		{#if badge.kind === 'category'}
			{@const CategoryIcon = categoryIcon(badge.category)}
			<Badge variant="secondary" class="min-w-0">
				<CategoryIcon aria-hidden="true" />
				<span class="truncate">{badge.label}</span>
			</Badge>
		{:else if badge.kind === 'stars'}
			<Badge variant="secondary" aria-label="{badge.stars} stars">
				<StarIcon aria-hidden="true" />
				<Number value={badge.stars} preset="compact" />
			</Badge>
		{:else if badge.kind === 'rating'}
			<Badge variant="secondary" aria-label="Rated {badge.average} out of 5">
				<ThumbsUpIcon aria-hidden="true" />
				<Number value={badge.average} maximumFractionDigits={1} />
			</Badge>
		{:else}
			<Badge variant="secondary" class="min-w-0" aria-label="By {badge.label}">
				<UserIcon aria-hidden="true" />
				<span class="truncate">{badge.label}</span>
			</Badge>
		{/if}
	{/each}
</div>
