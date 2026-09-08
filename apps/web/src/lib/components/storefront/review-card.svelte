<script lang="ts">
	import { Avatar, AvatarFallback, AvatarImage, Item, ItemContent, StarRating } from '@yuki/ui';
	import { initialsOf } from '$lib/initials.ts';
	import type { Review } from '$lib/server/reviews.ts';

	let { review }: { review: Review } = $props();

	const postedAt = $derived(new Date(review.createdAt));
</script>

<Item variant="outline" class="items-start">
	<Avatar class="size-9 shrink-0">
		{#if review.author.image}
			<AvatarImage src={review.author.image} alt="" />
		{/if}
		<AvatarFallback>{initialsOf(review.author.name)}</AvatarFallback>
	</Avatar>

	<ItemContent class="gap-1">
		<div class="flex flex-wrap items-center gap-x-2 gap-y-1">
			<span class="text-sm font-medium">{review.author.name}</span>
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
