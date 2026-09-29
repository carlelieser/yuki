<script lang="ts">
	import { onMount } from 'svelte';
	import { toast } from 'svelte-sonner';

	type Props = { title: string; description: string | null; variant: 'error' | 'success' };

	let { title, description, variant }: Props = $props();

	onMount(() => {
		if (description) toast[variant](title, { description });
	});
</script>
