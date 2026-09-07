<script lang="ts">
	import { Input } from '@yuki/ui';
	import SearchIcon from '@lucide/svelte/icons/search';

	let { class: className }: { class?: string } = $props();
</script>

<form role="search" class={className}>
	<div class="relative">
		<SearchIcon
			class="pointer-events-none absolute start-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
			aria-hidden="true"
		/>
		<Input type="search" name="q" placeholder="Search" aria-label="Search products" class="ps-8" />
	</div>
</form>
