<script lang="ts">
	import { Button } from '../button/index.ts';
	import { cn } from '../../../cn.ts';
	import ChevronDownIcon from '@lucide/svelte/icons/chevron-down';
	import ChevronUpIcon from '@lucide/svelte/icons/chevron-up';
	import type { Snippet } from 'svelte';

	let {
		collapsedHeight = 320,
		showMoreLabel = 'Show more',
		showLessLabel = 'Show less',
		fadeClass = 'from-background',
		class: className,
		children
	}: {
		collapsedHeight?: number;
		showMoreLabel?: string;
		showLessLabel?: string;
		fadeClass?: string;
		class?: string;
		children: Snippet;
	} = $props();

	let innerRef = $state<HTMLElement | null>(null);
	let contentHeight = $state(0);
	let isExpanded = $state(false);

	const isOverflowing = $derived(contentHeight > collapsedHeight);

	$effect(() => {
		const element = innerRef;
		if (element === null) return;

		const observer = new ResizeObserver(() => {
			contentHeight = element.getBoundingClientRect().height;
		});

		observer.observe(element);
		return () => observer.disconnect();
	});
</script>

<div class={cn('space-y-2', className)}>
	<div
		class="relative overflow-hidden"
		style:max-height={isExpanded ? undefined : `${collapsedHeight}px`}
	>
		<div bind:this={innerRef}>
			{@render children()}
		</div>

		{#if isOverflowing && !isExpanded}
			<div
				aria-hidden="true"
				class={cn(
					'pointer-events-none absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t to-transparent',
					fadeClass
				)}
			></div>
		{/if}
	</div>

	{#if isOverflowing}
		<Button variant="ghost" size="sm" onclick={() => (isExpanded = !isExpanded)}>
			{#if isExpanded}
				<ChevronUpIcon />
				{showLessLabel}
			{:else}
				<ChevronDownIcon />
				{showMoreLabel}
			{/if}
		</Button>
	{/if}
</div>
