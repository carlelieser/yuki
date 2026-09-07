<script lang="ts">
	import {
		Button,
		DropdownMenu,
		DropdownMenuContent,
		DropdownMenuRadioGroup,
		DropdownMenuRadioItem,
		DropdownMenuTrigger
	} from '@yuki/ui';
	import { setMode, userPrefersMode } from 'mode-watcher';
	import MoonIcon from '@lucide/svelte/icons/moon';
	import SunIcon from '@lucide/svelte/icons/sun';

	type Mode = typeof userPrefersMode.current;
</script>

<DropdownMenu>
	<DropdownMenuTrigger>
		{#snippet child({ props })}
			<Button {...props} variant="ghost" size="icon" class="relative" aria-label="Change theme">
				<SunIcon
					class="size-4 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0"
					aria-hidden="true"
				/>
				<MoonIcon
					class="absolute size-4 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100"
					aria-hidden="true"
				/>
			</Button>
		{/snippet}
	</DropdownMenuTrigger>

	<DropdownMenuContent align="end">
		<DropdownMenuRadioGroup
			value={userPrefersMode.current}
			onValueChange={(value) => setMode(value as Mode)}
		>
			<DropdownMenuRadioItem value="light">Light</DropdownMenuRadioItem>
			<DropdownMenuRadioItem value="dark">Dark</DropdownMenuRadioItem>
			<DropdownMenuRadioItem value="system">System</DropdownMenuRadioItem>
		</DropdownMenuRadioGroup>
	</DropdownMenuContent>
</DropdownMenu>
