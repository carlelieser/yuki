<script lang="ts">
	import '../app.css';
	import { ModeWatcher, userPrefersMode } from 'mode-watcher';
	import { Toaster } from 'svelte-sonner';
	import { SeoHead } from '$lib/components/seo/index.ts';
	import { siteMeta } from '$lib/seo/page-meta.ts';

	let { children } = $props();

	const defaults = siteMeta();
</script>

<SeoHead {...defaults} />

<ModeWatcher />
<Toaster theme={userPrefersMode.current} />
{@render children?.()}
