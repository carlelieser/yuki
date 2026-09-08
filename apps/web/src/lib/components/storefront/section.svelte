<script lang="ts">
	import { Separator } from '@yuki/ui';
	import type { Snippet } from 'svelte';

	let {
		title,
		action,
		hasSeparator = false,
		children
	}: {
		title?: string;
		action?: Snippet;
		hasSeparator?: boolean;
		children: Snippet;
	} = $props();
</script>

<section class="space-y-4">
	<header class="flex items-baseline justify-between gap-4">
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
