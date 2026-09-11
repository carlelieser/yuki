<script lang="ts">
	import { AspectRatio, Badge, Card, CardContent } from '@yuki/ui';
	import type { ResolvedPathname } from '$app/types';
	import type { Snippet } from 'svelte';

	let {
		title,
		href,
		badge,
		description,
		image,
		icon,
		badges,
		variant = 'default'
	}: {
		title: string;
		href: ResolvedPathname;
		badge?: string;
		description?: string | null;
		image?: Snippet;
		icon?: Snippet;
		badges?: Snippet;
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
				<div class="flex min-w-0 flex-1 flex-col gap-2">
					<div class="flex flex-col gap-0.5">
						<h3 class="line-clamp-1 text-sm font-medium group-hover/card:underline">{title}</h3>
						{#if description}
							<p class="line-clamp-1 text-xs text-muted-foreground">{description}</p>
						{/if}
					</div>
					{@render badges?.()}
				</div>
			</CardContent>
		{:else}
			<CardContent class="flex flex-col gap-2 p-3">
				<div class="flex flex-col gap-0.5">
					<h3 class="line-clamp-2 text-sm font-medium group-hover/card:underline">{title}</h3>
					{#if description}
						<p class="line-clamp-2 text-xs text-muted-foreground">{description}</p>
					{/if}
				</div>
				{@render badges?.()}
			</CardContent>
		{/if}
	</a>
</Card>
