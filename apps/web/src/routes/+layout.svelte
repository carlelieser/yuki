<script lang="ts">
	import '../app.css';
	import { ModeWatcher } from 'mode-watcher';

	let { children } = $props();
</script>

<svelte:head><title>Yuki</title></svelte:head>

<ModeWatcher />
{@render children?.()}
