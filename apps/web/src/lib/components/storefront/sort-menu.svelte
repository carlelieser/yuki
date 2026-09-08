<script lang="ts">
	import {
		Button,
		DropdownMenu,
		DropdownMenuContent,
		DropdownMenuRadioGroup,
		DropdownMenuRadioItem,
		DropdownMenuTrigger
	} from '@yuki/ui';
	import ArrowUpDownIcon from '@lucide/svelte/icons/arrow-up-down';

	type SortMenuOption = { value: string; label: string };

	let {
		options,
		value,
		onSelect
	}: {
		options: readonly SortMenuOption[];
		value: string;
		onSelect: (value: string) => void;
	} = $props();

	const label = $derived(options.find((option) => option.value === value)?.label ?? 'Sort');
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
		<DropdownMenuRadioGroup {value} onValueChange={onSelect}>
			{#each options as option (option.value)}
				<DropdownMenuRadioItem value={option.value}>{option.label}</DropdownMenuRadioItem>
			{/each}
		</DropdownMenuRadioGroup>
	</DropdownMenuContent>
</DropdownMenu>
