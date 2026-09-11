<script lang="ts">
	import { ToggleGroup, ToggleGroupItem } from '@yuki/ui';
	import LayoutGridIcon from '@lucide/svelte/icons/layout-grid';
	import ListIcon from '@lucide/svelte/icons/list';
	import { getViewModeStore } from '$lib/view-mode-store.svelte.ts';
	import { parseViewMode } from '$lib/view-mode.ts';

	const views = getViewModeStore();
</script>

<ToggleGroup
	type="single"
	value={views.mode}
	onValueChange={(value) => views.choose(parseViewMode(value))}
	aria-label="Change layout"
>
	<ToggleGroupItem value="grid" aria-label="Grid view">
		<LayoutGridIcon aria-hidden="true" />
	</ToggleGroupItem>
	<ToggleGroupItem value="list" aria-label="List view">
		<ListIcon aria-hidden="true" />
	</ToggleGroupItem>
</ToggleGroup>
