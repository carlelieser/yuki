<script lang="ts">
	import type { SubmitFunction } from '@sveltejs/kit';
	import { enhance } from '$app/forms';
	import { KeyRoundIcon } from '@lucide/svelte';
	import { toast } from 'svelte-sonner';
	import {
		Button,
		Item,
		ItemActions,
		ItemContent,
		ItemDescription,
		ItemMedia,
		ItemTitle
	} from '@yuki/ui';

	type Props = { hasPassword: boolean; email: string };

	let { hasPassword, email }: Props = $props();

	const announceEmail: SubmitFunction = () => {
		return async ({ result, update }) => {
			if (result.type === 'success') {
				toast.success('Check your email', {
					description: `We sent a link to ${email} to set your password.`
				});
			} else if (result.type === 'failure' || result.type === 'error') {
				toast.error('Could not send the email', { description: 'Try again in a moment.' });
			}

			await update();
		};
	};
</script>

<Item variant="outline">
	<ItemMedia variant="icon">
		<KeyRoundIcon class="size-4" />
	</ItemMedia>

	<ItemContent>
		<ItemTitle>Password</ItemTitle>
		<ItemDescription>{hasPassword ? 'Set' : 'Not set'}</ItemDescription>
	</ItemContent>

	{#if !hasPassword}
		<ItemActions>
			<form method="POST" action="?/setPassword" use:enhance={announceEmail}>
				<Button type="submit" variant="outline" size="sm">Set a password</Button>
			</form>
		</ItemActions>
	{/if}
</Item>
