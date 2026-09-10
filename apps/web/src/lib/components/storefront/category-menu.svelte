<script lang="ts">
	import {
		Button,
		DropdownMenu,
		DropdownMenuContent,
		DropdownMenuRadioGroup,
		DropdownMenuRadioItem,
		DropdownMenuTrigger
	} from '@yuki/ui';
	import LayoutGridIcon from '@lucide/svelte/icons/layout-grid';
	import { categoryLabel, type CategoryOption, type ListingCategory } from '$lib/categories.ts';
	import { categoryIcon } from '$lib/category-icons.ts';

	const ALL_CATEGORIES = 'all';

	let {
		categories,
		selected = null,
		onSelect
	}: {
		categories: CategoryOption[];
		selected?: ListingCategory | null;
		onSelect: (category: ListingCategory | null) => void;
	} = $props();

	const current = $derived(selected ?? ALL_CATEGORIES);
	const label = $derived(selected === null ? 'Category' : categoryLabel(selected));
	const TriggerIcon = $derived(selected === null ? LayoutGridIcon : categoryIcon(selected));

	function select(value: string): void {
		onSelect(value === ALL_CATEGORIES ? null : (value as ListingCategory));
	}
</script>

<DropdownMenu>
	<DropdownMenuTrigger>
		{#snippet child({ props })}
			<Button {...props} variant="ghost" aria-label="Filter listings by category">
				<TriggerIcon aria-hidden="true" />
				{label}
			</Button>
		{/snippet}
	</DropdownMenuTrigger>

	<DropdownMenuContent align="end" class="w-52">
		<DropdownMenuRadioGroup value={current} onValueChange={select}>
			<DropdownMenuRadioItem value={ALL_CATEGORIES}>
				<LayoutGridIcon aria-hidden="true" class="text-muted-foreground" />
				All
			</DropdownMenuRadioItem>
			{#each categories as category (category.value)}
				{@const Icon = categoryIcon(category.value)}
				<DropdownMenuRadioItem value={category.value}>
					<Icon aria-hidden="true" class="text-muted-foreground" />
					{category.label}
				</DropdownMenuRadioItem>
			{/each}
		</DropdownMenuRadioGroup>
	</DropdownMenuContent>
</DropdownMenu>
