<script lang="ts">
	import { resolve } from '$app/paths';
	import { buttonVariants, cn } from '@yuki/ui';
	import ModeToggle from './mode-toggle.svelte';
	import SiteLogo from './site-logo.svelte';
	import SiteSearch from './site-search.svelte';
	import UserMenu from './user-menu.svelte';

	type Props = {
		user?: { name: string; email: string; image: string | null } | null;
		onopensearch: () => void;
	};

	let { user = null, onopensearch }: Props = $props();
</script>

<header class="sticky top-0 z-50 w-full border-b bg-background">
	<div class="mx-auto flex h-14 w-full max-w-6xl items-center gap-1 px-4">
		<SiteLogo hasResponsiveLabel />
		<SiteSearch class="ms-auto" onopen={onopensearch} />

		<ModeToggle />

		{#if user}
			<UserMenu {user} />
		{:else}
			<nav class="flex items-center gap-1">
				<a
					href={resolve('/signin')}
					class={cn(buttonVariants({ variant: 'ghost', size: 'sm' }), 'hidden sm:inline-flex')}
				>
					Sign in
				</a>
				<a href={resolve('/signup')} class={buttonVariants({ size: 'sm' })}>Get started</a>
			</nav>
		{/if}
	</div>
</header>
