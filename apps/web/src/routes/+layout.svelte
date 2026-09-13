<script lang="ts">
	import '../app.css';
	import { ModeWatcher, userPrefersMode } from 'mode-watcher';
	import { Toaster } from 'svelte-sonner';
	let { children } = $props();
</script>

<ModeWatcher />
<Toaster theme={userPrefersMode.current} />
{@render children?.()}
