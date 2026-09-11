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
			<div class="flex min-w-0 flex-1 flex-col gap-2">
				<div class="flex flex-col gap-0.5">
					<Skeleton class="h-4 w-3/4" />
					<Skeleton class="h-3 w-full" />
				</div>
				<Skeleton class="h-5 w-full rounded-4xl" />
			</div>
		</CardContent>
	{:else}
		<CardContent class="flex flex-col gap-2 p-3">
			<div class="flex flex-col gap-0.5">
				<Skeleton class="h-4 w-3/4" />
				<Skeleton class="h-3 w-full" />
			</div>
			<Skeleton class="h-5 w-full rounded-4xl" />
			<Skeleton class="h-5 w-2/3 rounded-4xl" />
		</CardContent>
	{/if}
</Card>
