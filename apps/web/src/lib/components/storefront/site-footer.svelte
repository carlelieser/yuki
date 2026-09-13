<script lang="ts">
	import { resolve } from '$app/paths';
	import { cn } from '@yuki/ui';
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

	const year = new Date().getFullYear();
</script>

<footer class={cn('w-full border-t', className)}>
	<div
		class={cn(
			'mx-auto flex w-full flex-col gap-4 px-4 py-8',
			'sm:flex-row sm:items-center sm:justify-between',
			widths[width]
		)}
	>
		<div class="flex flex-col gap-2 sm:gap-1">
			<SiteLogo size="sm" />
			<p class="text-sm text-muted-foreground">
				© {year} Yuki. Listings link to software published by third parties.
			</p>
		</div>

		<nav class="flex items-center gap-4">
			{#each links as link (link.href)}
				<a
					href={link.href}
					class={cn(
						'rounded-sm text-sm text-muted-foreground transition-colors hover:text-foreground',
						'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring'
					)}
				>
					{link.label}
				</a>
			{/each}
		</nav>
	</div>
</footer>
