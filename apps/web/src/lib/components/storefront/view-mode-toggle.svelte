<script lang="ts">
	import { Button, ButtonGroup } from '@yuki/ui';
	import LayoutGridIcon from '@lucide/svelte/icons/layout-grid';
	import ListIcon from '@lucide/svelte/icons/list';
	import { getViewModeStore } from '$lib/view-mode-store.svelte.ts';

	const views = getViewModeStore();
</script>

<ButtonGroup aria-label="Change layout">
	<Button
		variant="outline"
		size="icon-sm"
		aria-pressed={views.mode === 'grid'}
		class="aria-pressed:bg-muted aria-pressed:text-foreground"
		onclick={() => views.choose('grid')}
	>
		<LayoutGridIcon aria-hidden="true" />
		<span class="sr-only">Grid view</span>
	</Button>
	<Button
		variant="outline"
		size="icon-sm"
		aria-pressed={views.mode === 'list'}
		class="aria-pressed:bg-muted aria-pressed:text-foreground"
		onclick={() => views.choose('list')}
	>
		<ListIcon aria-hidden="true" />
		<span class="sr-only">List view</span>
	</Button>
</ButtonGroup>
