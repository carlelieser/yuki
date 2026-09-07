<script lang="ts">
	import { resolve } from '$app/paths';
	import { buttonVariants } from '@yuki/ui';
	import ModeToggle from './mode-toggle.svelte';
	import SiteSearch from './site-search.svelte';
	import UserMenu from './user-menu.svelte';

	type Props = {
		user?: { name: string; email: string; image: string | null } | null;
		onopensearch: () => void;
	};

	let { user = null, onopensearch }: Props = $props();
</script>

<header class="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur">
	<div class="mx-auto flex h-14 w-full max-w-6xl items-center gap-4 px-4">
		<a href={resolve('/')} class="text-lg font-semibold tracking-tight">Yuki</a>
		<SiteSearch class="ms-auto" onopen={onopensearch} />

		<ModeToggle />

		{#if user}
			<UserMenu {user} />
		{:else}
			<nav class="flex items-center gap-2">
				<a href={resolve('/signin')} class={buttonVariants({ variant: 'ghost', size: 'sm' })}>
					Sign in
				</a>
				<a href={resolve('/signup')} class={buttonVariants({ size: 'sm' })}>Sign up</a>
			</nav>
		{/if}
	</div>
</header>
