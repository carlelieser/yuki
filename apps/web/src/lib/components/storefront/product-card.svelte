<script lang="ts">
	import { AspectRatio, Badge, Card, CardContent } from '@yuki/ui';
	import type { ResolvedPathname } from '$app/types';
	import type { Snippet } from 'svelte';

	let {
		title,
		href,
		badge,
		image,
		meta
	}: {
		title: string;
		href: ResolvedPathname;
		badge?: string;
		image?: Snippet;
		meta?: Snippet;
	} = $props();
</script>

<Card class="group/card overflow-hidden py-0">
	<a {href} class="block focus-visible:outline-none">
		<div class="relative">
			<AspectRatio ratio={1}>
				{#if image}
					{@render image()}
				{:else}
					<div class="size-full bg-muted"></div>
				{/if}
			</AspectRatio>
			{#if badge}
				<Badge class="absolute start-2 top-2">{badge}</Badge>
			{/if}
		</div>
		<CardContent class="space-y-1 p-3">
			<h3 class="line-clamp-2 text-sm font-medium group-hover/card:underline">{title}</h3>
			{@render meta?.()}
		</CardContent>
	</a>
</Card>
