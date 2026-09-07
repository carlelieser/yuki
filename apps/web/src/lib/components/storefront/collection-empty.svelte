<script lang="ts">
	import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@yuki/ui';
	import PackageOpenIcon from '@lucide/svelte/icons/package-open';

	let {
		title = 'Nothing here yet',
		description = 'Check back soon for new arrivals.'
	}: { title?: string; description?: string } = $props();
</script>

<Empty class="border">
	<EmptyHeader>
		<EmptyMedia variant="icon"><PackageOpenIcon /></EmptyMedia>
		<EmptyTitle>{title}</EmptyTitle>
		<EmptyDescription>{description}</EmptyDescription>
	</EmptyHeader>
</Empty>
