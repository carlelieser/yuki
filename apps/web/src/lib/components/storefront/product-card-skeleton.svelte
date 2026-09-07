<script lang="ts">
	import { AspectRatio, Card, CardContent, Skeleton } from '@yuki/ui';
</script>

<Card class="overflow-hidden py-0">
	<AspectRatio ratio={1}>
		<Skeleton class="size-full rounded-none" />
	</AspectRatio>
	<CardContent class="space-y-2 p-3">
		<Skeleton class="h-4 w-3/4" />
		<Skeleton class="h-4 w-1/2" />
	</CardContent>
</Card>
