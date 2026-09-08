<script lang="ts">
	import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@yuki/ui';
	import PackageOpenIcon from '@lucide/svelte/icons/package-open';
	import type { Snippet } from 'svelte';

	let {
		title = 'Nothing here yet',
		description = 'Check back soon for new arrivals.',
		icon
	}: { title?: string; description?: string; icon?: Snippet } = $props();
</script>

<Empty class="border">
	<EmptyHeader>
		<EmptyMedia variant="icon">
			{#if icon}
				{@render icon()}
			{:else}
				<PackageOpenIcon />
			{/if}
		</EmptyMedia>
		<EmptyTitle>{title}</EmptyTitle>
		<EmptyDescription>{description}</EmptyDescription>
	</EmptyHeader>
</Empty>
