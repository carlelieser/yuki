<script lang="ts">
	import { RadioGroup as RadioGroupPrimitive } from 'bits-ui';
	import StarIcon from '@lucide/svelte/icons/star';
	import { cn } from '../../../cn.ts';

	let {
		value = $bindable(0),
		name,
		max = 5,
		disabled = false,
		class: className,
		...restProps
	}: Omit<RadioGroupPrimitive.RootProps, 'value' | 'children'> & {
		value?: number;
		name: string;
		max?: number;
	} = $props();

	let hovered = $state(0);

	const options = $derived(Array.from({ length: max }, (_, index) => index + 1));
	const active = $derived(hovered || value);

	function label(rating: number): string {
		return rating === 1 ? '1 star' : `${rating} stars`;
	}
</script>

<RadioGroupPrimitive.Root
	bind:value={
		() => String(value),
		(next) => {
			value = Number(next);
		}
	}
	{name}
	{disabled}
	data-slot="star-rating-input"
	class={cn('flex w-fit items-center gap-0.5', className)}
	onmouseleave={() => (hovered = 0)}
	{...restProps}
>
	{#each options as option (option)}
		<RadioGroupPrimitive.Item
			value={String(option)}
			title={label(option)}
			aria-label={label(option)}
			class="rounded-sm text-muted-foreground/40 outline-none focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
			onmouseenter={() => (hovered = option)}
			onfocus={() => (hovered = option)}
			onblur={() => (hovered = 0)}
		>
			<StarIcon class={cn('size-6', option <= active && 'fill-current text-primary')} />
		</RadioGroupPrimitive.Item>
	{/each}
</RadioGroupPrimitive.Root>
