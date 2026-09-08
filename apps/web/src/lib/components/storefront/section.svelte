<script lang="ts">
	import { cn, Separator } from '@yuki/ui';
	import type { Snippet } from 'svelte';

	let {
		title,
		action,
		hasSeparator = false,
		isHeaderSticky = false,
		children
	}: {
		title?: string;
		action?: Snippet;
		hasSeparator?: boolean;
		isHeaderSticky?: boolean;
		children: Snippet;
	} = $props();
</script>

<section class="space-y-4">
	<header
		class={cn(
			'flex items-baseline justify-between gap-4',
			isHeaderSticky && 'sticky top-14 z-40 -mx-4 bg-background px-4 py-3'
		)}
	>
		{#if title}
			<h2 class="text-xl font-semibold tracking-tight">{title}</h2>
		{/if}
		{@render action?.()}
	</header>
	{#if hasSeparator}
		<Separator />
	{/if}
	{@render children()}
</section>
