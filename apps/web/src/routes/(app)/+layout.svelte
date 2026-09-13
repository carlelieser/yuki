<script lang="ts">
	import SearchDialog from '$lib/components/storefront/search-dialog.svelte';
	import SiteFooter from '$lib/components/storefront/site-footer.svelte';
	import SiteHeader from '$lib/components/storefront/site-header.svelte';
	import { createArchitectureStore } from '$lib/architecture-store.svelte.ts';
	import { createViewModeStore } from '$lib/view-mode-store.svelte.ts';

	let { children, data } = $props();

	const architectures = createArchitectureStore(
		() => data.user?.architecture ?? null,
		() => data.user !== null
	);

	createViewModeStore(() => data.viewMode);

	$effect(() => {
		architectures.loadLocal();
	});

	let isSearchOpen = $state(false);

	function handleKeydown(event: KeyboardEvent): void {
		if (event.key !== 'k' || !(event.metaKey || event.ctrlKey)) return;
		event.preventDefault();
		isSearchOpen = !isSearchOpen;
	}
</script>

<svelte:window onkeydown={handleKeydown} />

<div class="flex min-h-screen flex-col">
	<SiteHeader user={data.user} onopensearch={() => (isSearchOpen = true)} />
	<div class="flex flex-1 flex-col">
		{@render children?.()}
	</div>
	<SiteFooter />
</div>

<SearchDialog bind:open={isSearchOpen} />
