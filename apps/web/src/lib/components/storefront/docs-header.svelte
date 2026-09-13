<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { Button } from '@yuki/ui';
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
				{@const isActive = page.url.pathname === link.href}
				<Button
					href={link.href}
					variant="ghost"
					aria-current={isActive ? 'page' : undefined}
					class={isActive ? 'text-foreground' : 'text-muted-foreground'}
				>
					{link.label}
				</Button>
			{/each}
		</nav>

		<ModeToggle />
	</div>
</header>
