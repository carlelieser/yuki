<script lang="ts" module>
	export type NumberFormatPreset = 'decimal' | 'integer' | 'compact' | 'percent' | 'currency';

	export type NumberFormatOptions = Intl.NumberFormatOptions & {
		preset?: NumberFormatPreset;
		locale?: string | string[];
	};

	const presets = {
		decimal: {},
		integer: { maximumFractionDigits: 0 },
		compact: { notation: 'compact', maximumFractionDigits: 1 },
		percent: { style: 'percent', maximumFractionDigits: 0 },
		currency: { style: 'currency', currencyDisplay: 'narrowSymbol' }
	} satisfies Record<NumberFormatPreset, Intl.NumberFormatOptions>;

	const cache = new Map<string, Intl.NumberFormat>();

	function defined<T extends object>(options: T): T {
		return Object.fromEntries(
			Object.entries(options).filter(([, value]) => value !== undefined)
		) as T;
	}

	export function formatNumber(value: number | bigint, options: NumberFormatOptions = {}) {
		const { preset = 'decimal', locale, ...overrides } = defined(options);
		const key = `${JSON.stringify(locale ?? null)}|${preset}|${JSON.stringify(overrides)}`;

		let formatter = cache.get(key);
		if (!formatter) {
			formatter = new Intl.NumberFormat(locale, { ...presets[preset], ...overrides });
			cache.set(key, formatter);
		}

		return formatter.format(value);
	}
</script>

<script lang="ts">
	import { cn, type WithElementRef, type WithoutChildren } from '../../../cn.ts';
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
