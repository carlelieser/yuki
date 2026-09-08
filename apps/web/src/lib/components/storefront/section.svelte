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

	const STICKY_OFFSET = 56;

	let sentinel = $state<HTMLDivElement | null>(null);
	let isStuck = $state(false);

	$effect(() => {
		if (sentinel === null) return;

		const observer = new IntersectionObserver(
			(entries) => {
				const entry = entries[0];
				if (entry !== undefined) isStuck = !entry.isIntersecting;
			},
			{ rootMargin: `-${STICKY_OFFSET}px 0px 0px 0px`, threshold: 0 }
		);

		observer.observe(sentinel);
		return () => observer.disconnect();
	});
</script>

<section class="space-y-4">
	{#if isHeaderSticky}
		<div bind:this={sentinel} aria-hidden="true" class="-mb-4 h-px"></div>
	{/if}
	<header
		class={cn(
			'relative flex items-baseline justify-between gap-4',
			isHeaderSticky && 'sticky top-14 z-40 isolate py-3',
			isHeaderSticky &&
				'before:pointer-events-none before:absolute before:inset-y-0 before:left-1/2 before:-z-10 before:w-[100dvw] before:-translate-x-1/2 before:bg-background before:transition-shadow before:duration-200',
			isHeaderSticky && isStuck && 'before:shadow-sm'
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
