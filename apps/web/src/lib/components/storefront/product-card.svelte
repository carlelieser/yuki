<script lang="ts">
	import { AspectRatio, Badge, Card, CardContent } from '@yuki/ui';
	import type { ResolvedPathname } from '$app/types';
	import type { Snippet } from 'svelte';

	let {
		title,
		href,
		badge,
		image,
		icon,
		meta,
		variant = 'default'
	}: {
		title: string;
		href: ResolvedPathname;
		badge?: string;
		image?: Snippet;
		icon?: Snippet;
		meta?: Snippet;
		variant?: 'default' | 'wide';
	} = $props();
</script>

<Card class="group/card overflow-hidden py-0">
	<a {href} class="block focus-visible:outline-none">
		<div class="relative">
			<AspectRatio ratio={variant === 'wide' ? 16 / 9 : 1}>
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
		{#if variant === 'wide'}
			<CardContent class="flex items-center gap-3 p-3">
				<div class="size-10 shrink-0 overflow-hidden rounded-lg border bg-muted">
					{@render icon?.()}
				</div>
				<div class="min-w-0 flex-1">
					<h3 class="line-clamp-1 text-sm font-medium group-hover/card:underline">{title}</h3>
					{@render meta?.()}
				</div>
			</CardContent>
		{:else}
			<CardContent class="p-3">
				<h3 class="line-clamp-2 text-sm font-medium group-hover/card:underline">{title}</h3>
				{@render meta?.()}
			</CardContent>
		{/if}
	</a>
</Card>
