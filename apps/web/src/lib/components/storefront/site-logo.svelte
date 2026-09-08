<script lang="ts">
	import { resolve } from '$app/paths';
	import { cn } from '@yuki/ui';
	import type { ResolvedPathname } from '$app/types';

	let {
		href = resolve('/'),
		size = 'md',
		showLabel = true,
		label = 'Yuki',
		class: className
	}: {
		href?: ResolvedPathname | null;
		size?: 'sm' | 'md' | 'lg';
		showLabel?: boolean;
		label?: string;
		class?: string;
	} = $props();

	const iconSizes = {
		sm: 'size-6',
		md: 'size-7',
		lg: 'size-9'
	};

	const labelSizes = {
		sm: 'text-base',
		md: 'text-lg',
		lg: 'text-2xl'
	};
</script>

{#snippet content()}
	<img
		src="/logo.png"
		alt={showLabel ? '' : label}
		width="128"
		height="128"
		class={cn(
			'shrink-0 rounded-full bg-secondary object-contain',
			'transition-transform duration-200 group-hover:-rotate-24',
			iconSizes[size]
		)}
	/>
	{#if showLabel}
		<span class={cn('font-semibold tracking-tight', labelSizes[size])}>{label}</span>
	{/if}
{/snippet}

{#if href === null}
	<span class={cn('flex items-center gap-1 group', className)}>
		{@render content()}
	</span>
{:else}
	<a
		{href}
		class={cn(
			'group flex items-center gap-1 rounded-sm',
			'focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring',
			className
		)}
	>
		{@render content()}
	</a>
{/if}
