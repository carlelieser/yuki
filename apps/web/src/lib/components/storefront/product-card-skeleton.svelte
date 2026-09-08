<script lang="ts">
	import { AspectRatio, Card, CardContent, Skeleton } from '@yuki/ui';

	let { variant = 'default' }: { variant?: 'default' | 'wide' } = $props();
</script>

<Card class="overflow-hidden py-0">
	<AspectRatio ratio={variant === 'wide' ? 16 / 9 : 1}>
		<Skeleton class="size-full rounded-none" />
	</AspectRatio>
	{#if variant === 'wide'}
		<CardContent class="flex items-center gap-3 p-3">
			<Skeleton class="size-10 shrink-0 rounded-lg" />
			<div class="min-w-0 flex-1 space-y-2">
				<Skeleton class="h-4 w-3/4" />
				<Skeleton class="h-4 w-1/2" />
			</div>
		</CardContent>
	{:else}
		<CardContent class="space-y-2 p-3">
			<Skeleton class="h-4 w-3/4" />
			<Skeleton class="h-4 w-1/2" />
		</CardContent>
	{/if}
</Card>
