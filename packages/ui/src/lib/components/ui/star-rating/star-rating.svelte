<script lang="ts">
	import StarIcon from '@lucide/svelte/icons/star';
	import { cn } from '../../../cn.ts';

	let {
		value,
		max = 5,
		size = 'default',
		class: className
	}: {
		value: number;
		max?: number;
		size?: 'sm' | 'default';
		class?: string;
	} = $props();

	const clamped = $derived(Math.min(Math.max(value, 0), max));
	const percentage = $derived(max === 0 ? 0 : (clamped / max) * 100);
	const stars = $derived(Array.from({ length: max }, (_, index) => index));
	const iconSize = $derived(size === 'sm' ? 'size-3' : 'size-4');
</script>

<span
	class={cn('relative inline-flex w-fit shrink-0 align-middle', className)}
	role="img"
	aria-label="{clamped} out of {max} stars"
>
	<span class="flex" aria-hidden="true">
		{#each stars as star (star)}
			<StarIcon class={cn(iconSize, 'text-muted-foreground/40')} />
		{/each}
	</span>

	<span
		class="absolute inset-y-0 start-0 flex overflow-hidden"
		style="width: {percentage}%"
		aria-hidden="true"
	>
		{#each stars as star (star)}
			<StarIcon class={cn(iconSize, 'shrink-0 fill-current text-primary')} />
		{/each}
	</span>
</span>
