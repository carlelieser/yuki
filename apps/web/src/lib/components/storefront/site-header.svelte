<script lang="ts">
	import { resolve } from '$app/paths';
	import SiteSearch from './site-search.svelte';
</script>

<header class="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur">
	<div class="mx-auto flex h-14 w-full max-w-6xl items-center gap-4 px-4">
		<a href={resolve('/')} class="text-lg font-semibold tracking-tight">Yuki</a>
		<SiteSearch class="ms-auto w-full max-w-xs" />
	</div>
</header>
