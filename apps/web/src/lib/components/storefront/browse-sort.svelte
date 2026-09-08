<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import {
		Button,
		DropdownMenu,
		DropdownMenuContent,
		DropdownMenuRadioGroup,
		DropdownMenuRadioItem,
		DropdownMenuTrigger
	} from '@yuki/ui';
	import ArrowUpDownIcon from '@lucide/svelte/icons/arrow-up-down';
	import {
		BROWSE_SORT_OPTIONS,
		fromSortValue,
		toSortValue,
		type BrowseOrder,
		type BrowseSort
	} from '$lib/browse.ts';

	let { sort, order }: { sort: BrowseSort; order: BrowseOrder } = $props();

	const current = $derived(toSortValue({ sort, order }));
	const label = $derived(
		BROWSE_SORT_OPTIONS.find((option) => option.value === current)?.label ?? 'Sort'
	);

	function select(value: string): void {
		const selected = fromSortValue(value);
		void goto(resolve(`/?sort=${selected.sort}&order=${selected.order}`), {
			keepFocus: true,
			noScroll: true
		});
	}
</script>

<DropdownMenu>
	<DropdownMenuTrigger>
		{#snippet child({ props })}
			<Button {...props} variant="ghost" aria-label="Change sort order">
				<ArrowUpDownIcon aria-hidden="true" />
				{label}
			</Button>
		{/snippet}
	</DropdownMenuTrigger>

	<DropdownMenuContent align="end" class="w-52">
		<DropdownMenuRadioGroup value={current} onValueChange={select}>
			{#each BROWSE_SORT_OPTIONS as option (option.value)}
				<DropdownMenuRadioItem value={option.value}>{option.label}</DropdownMenuRadioItem>
			{/each}
		</DropdownMenuRadioGroup>
	</DropdownMenuContent>
</DropdownMenu>
