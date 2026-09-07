<script lang="ts">
	import SearchDialog from '$lib/components/storefront/search-dialog.svelte';
	import SiteHeader from '$lib/components/storefront/site-header.svelte';

	let { children, data } = $props();

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
	{@render children?.()}
</div>

<SearchDialog bind:open={isSearchOpen} />
