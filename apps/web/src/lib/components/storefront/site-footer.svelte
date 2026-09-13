<script lang="ts">
	import { resolve } from '$app/paths';
	import { cn, Link } from '@yuki/ui';
	import SiteLogo from './site-logo.svelte';

	let { width = 'wide', class: className }: { width?: 'wide' | 'narrow'; class?: string } =
		$props();

	const links = [
		{ href: resolve('/(docs)/terms'), label: 'Terms' },
		{ href: resolve('/(docs)/privacy'), label: 'Privacy' }
	];

	const widths = {
		wide: 'max-w-6xl',
		narrow: 'max-w-3xl'
	};
</script>

<footer class={cn('w-full border-t', className)}>
	<div
		class={cn(
			'mx-auto flex w-full gap-4 px-4 py-8',
			'flex-row items-center justify-between',
			widths[width]
		)}
	>
		<SiteLogo size="sm" />
		<nav class="flex items-center gap-4 text-sm">
			{#each links as link (link.href)}
				<Link href={link.href} class="text-muted-foreground hover:text-foreground">
					{link.label}
				</Link>
			{/each}
		</nav>
	</div>
</footer>
