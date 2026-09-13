<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { cn } from '@yuki/ui';
	import ModeToggle from './mode-toggle.svelte';
	import SiteLogo from './site-logo.svelte';

	const links = [
		{ href: resolve('/(docs)/terms'), label: 'Terms' },
		{ href: resolve('/(docs)/privacy'), label: 'Privacy' }
	];
</script>

<header class="sticky top-0 z-50 w-full border-b bg-background">
	<div class="mx-auto flex h-14 w-full max-w-3xl items-center gap-4 px-4">
		<SiteLogo hasResponsiveLabel />

		<nav class="ms-auto flex items-center gap-1">
			{#each links as link (link.href)}
				<a
					href={link.href}
					aria-current={page.url.pathname === link.href ? 'page' : undefined}
					class={cn(
						'rounded-sm px-3 py-1.5 text-sm transition-colors',
						'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring',
						page.url.pathname === link.href
							? 'font-medium text-foreground'
							: 'text-muted-foreground hover:text-foreground'
					)}
				>
					{link.label}
				</a>
			{/each}
		</nav>

		<ModeToggle />
	</div>
</header>
