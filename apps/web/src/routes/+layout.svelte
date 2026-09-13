<script lang="ts">
	import '../app.css';
	import { ModeWatcher, userPrefersMode } from 'mode-watcher';
	import { Toaster } from 'svelte-sonner';

	let { children } = $props();
</script>

<svelte:head><title>Yuki</title></svelte:head>

<ModeWatcher />
<Toaster theme={userPrefersMode.current} />
{@render children?.()}
