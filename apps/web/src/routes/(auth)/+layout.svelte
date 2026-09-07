<script lang="ts">
	let { children } = $props();
</script>

<div class="flex min-h-svh flex-col items-center justify-center px-4 py-12">
	{@render children?.()}
</div>
