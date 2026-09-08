<script lang="ts">
	import { Item, ItemContent, StarRating, UserAvatar } from '@yuki/ui';
	import { initialsOf } from '$lib/initials.ts';
	import type { Review } from '$lib/server/reviews.ts';

	let { review }: { review: Review } = $props();

	const postedAt = $derived(new Date(review.createdAt));
</script>

<Item variant="outline" class="flex-col items-start">
	<div class="flex flex-row items-center gap-2">
		<UserAvatar initials={initialsOf(review.author.name)} image={review.author.image} />
		<span class="text-sm font-medium">{review.author.name}</span>
	</div>

	<ItemContent class="gap-2">
		<div class="flex flex-wrap items-center gap-x-2 gap-y-1">
			<StarRating value={review.rating} size="sm" />
			<time datetime={postedAt.toISOString()} class="text-xs text-muted-foreground">
				{postedAt.toLocaleDateString()}
			</time>
		</div>

		{#if review.body}
			<p class="text-sm whitespace-pre-line text-muted-foreground">{review.body}</p>
		{/if}
	</ItemContent>
</Item>
