<script lang="ts">
	import '../app.css';
	import SiteHeader from '$lib/components/storefront/site-header.svelte';

	let { children, data } = $props();
</script>

<div class="flex min-h-screen flex-col">
	<SiteHeader user={data.user} />
	{@render children?.()}
</div>
