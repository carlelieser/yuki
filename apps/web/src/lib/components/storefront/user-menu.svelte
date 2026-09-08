<script lang="ts">
	import { resolve } from '$app/paths';
	import { LogOutIcon } from '@lucide/svelte';
	import {
		Avatar,
		AvatarFallback,
		AvatarImage,
		Button,
		DropdownMenu,
		DropdownMenuContent,
		DropdownMenuItem,
		DropdownMenuLabel,
		DropdownMenuSeparator,
		DropdownMenuTrigger
	} from '@yuki/ui';

	type Props = {
		user: { name: string; email: string; image: string | null };
	};

	let { user }: Props = $props();

	const initials = $derived(
		user.name
			.split(' ')
			.filter(Boolean)
			.slice(0, 2)
			.map((part) => part[0]?.toUpperCase() ?? '')
			.join('') || user.email[0]?.toUpperCase()
	);
</script>

<DropdownMenu>
	<DropdownMenuTrigger>
		{#snippet child({ props })}
			<Button {...props} variant="ghost" class="size-9 rounded-full p-0" aria-label="Account menu">
				<Avatar class="size-9">
					{#if user.image}
						<AvatarImage src={user.image} alt="" />
					{/if}
					<AvatarFallback>{initials}</AvatarFallback>
				</Avatar>
			</Button>
		{/snippet}
	</DropdownMenuTrigger>

	<DropdownMenuContent align="end" class="w-56">
		<DropdownMenuLabel class="grid gap-1">
			<span class="truncate text-sm font-medium">{user.name}</span>
			<span class="truncate text-xs font-normal text-muted-foreground">{user.email}</span>
		</DropdownMenuLabel>

		<DropdownMenuSeparator />

		<form method="POST" action={resolve('/signout')}>
			<DropdownMenuItem>
				{#snippet child({ props })}
					<button
						{...props}
						type="submit"
						class="w-full text-left flex items-center justify-between text-sm p-2"
					>
						Sign out
						<LogOutIcon class="size-4 text-muted-foreground" />
					</button>
				{/snippet}
			</DropdownMenuItem>
		</form>
	</DropdownMenuContent>
</DropdownMenu>
