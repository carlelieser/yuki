<script lang="ts">
	import { cn, type WithElementRef } from '../../../cn.ts';
	import { getEmblaContext } from './context.js';
	import type { HTMLAttributes } from 'svelte/elements';

	let {
		ref = $bindable(null),
		class: className,
		...restProps
	}: WithElementRef<HTMLAttributes<HTMLDivElement>> = $props();

	const emblaCtx = getEmblaContext('<Carousel.Dots/>');
</script>

{#if emblaCtx.scrollSnaps.length > 1}
	<div
		bind:this={ref}
		data-slot="carousel-dots"
		class={cn('flex items-center justify-center gap-2', className)}
		{...restProps}
	>
		{#each { length: emblaCtx.scrollSnaps.length }, index (index)}
			<button
				type="button"
				aria-label={`Go to slide ${index + 1}`}
				aria-current={emblaCtx.selectedIndex === index}
				class={cn(
					'size-2 rounded-full transition-colors',
					'focus-visible:ring-ring/50 focus-visible:ring-[3px] focus-visible:outline-none',
					emblaCtx.selectedIndex === index ? 'bg-primary' : 'bg-primary/25 hover:bg-primary/40'
				)}
				onclick={() => emblaCtx.scrollTo(index)}
			></button>
		{/each}
	</div>
{/if}
