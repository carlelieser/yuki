<script lang="ts">
	import { cn, type WithElementRef, type WithoutChildren } from '../../../cn.ts';
	import { formatNumber, type NumberFormatOptions } from './format-number.ts';
	import type { HTMLAttributes } from 'svelte/elements';

	let {
		ref = $bindable(null),
		value,
		preset = 'decimal',
		locale,
		currency,
		currencyDisplay,
		notation,
		signDisplay,
		unit,
		unitDisplay,
		minimumFractionDigits,
		maximumFractionDigits,
		minimumIntegerDigits,
		minimumSignificantDigits,
		maximumSignificantDigits,
		useGrouping,
		fallback = '—',
		exactTitle = true,
		title,
		class: className,
		...restProps
	}: WithoutChildren<WithElementRef<HTMLAttributes<HTMLElement>>> &
		NumberFormatOptions & {
			value: number | bigint | string | null | undefined;
			fallback?: string;
			exactTitle?: boolean;
		} = $props();

	const numeric = $derived(typeof value === 'string' ? Number(value) : value);

	const valid = $derived(
		(typeof numeric === 'number' && Number.isFinite(numeric)) || typeof numeric === 'bigint'
	);

	const options = $derived<NumberFormatOptions>({
		preset,
		locale,
		currency,
		currencyDisplay,
		notation,
		signDisplay,
		unit,
		unitDisplay,
		minimumFractionDigits,
		maximumFractionDigits,
		minimumIntegerDigits,
		minimumSignificantDigits,
		maximumSignificantDigits,
		useGrouping
	});

	const formatted = $derived(valid ? formatNumber(numeric!, options) : fallback);

	const exact = $derived(
		valid && exactTitle
			? formatNumber(numeric!, {
					...options,
					preset: preset === 'compact' ? 'decimal' : preset,
					notation: notation === 'compact' ? 'standard' : notation,
					maximumFractionDigits: undefined,
					minimumFractionDigits: undefined
				})
			: undefined
	);
</script>

<span
	bind:this={ref}
	data-slot="number"
	title={title ?? (exact !== undefined && exact !== formatted ? exact : undefined)}
	class={cn('tabular-nums', className)}
	{...restProps}
>
	{formatted}
</span>
