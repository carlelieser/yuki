<script lang="ts">
	import type { SubmitFunction } from '@sveltejs/kit';
	import { enhance } from '$app/forms';
	import { toast } from 'svelte-sonner';
	import {
		Button,
		GithubIcon,
		Item,
		ItemActions,
		ItemContent,
		ItemDescription,
		ItemMedia,
		ItemTitle,
		Link
	} from '@yuki/ui';

	type GithubAccount = { id: string; username: string | null; profileUrl: string | null };

	type Props = { github: GithubAccount | null; canUnlink: boolean };

	let { github, canUnlink }: Props = $props();

	const announceUnlink: SubmitFunction = () => {
		return async ({ result, update }) => {
			if (result.type === 'failure') {
				toast.error('Could not disconnect GitHub', {
					description: String(result.data?.unlinkError ?? '')
				});
			} else if (result.type === 'success') {
				toast.success('GitHub disconnected');
			}

			await update();
		};
	};
</script>

<Item variant="outline">
	<ItemMedia variant="icon">
		<GithubIcon class="size-4" />
	</ItemMedia>

	<ItemContent>
		<ItemTitle>GitHub</ItemTitle>
		<ItemDescription>
			{#if !github}
				Not connected
			{:else if github.username && github.profileUrl}
				<Link href={github.profileUrl} target="_blank" rel="noopener noreferrer">
					@{github.username}
				</Link>
			{:else}
				Connected
			{/if}
		</ItemDescription>
		{#if github && !canUnlink}
			<ItemDescription>
				GitHub is your only way to sign in. Set a password to disconnect it.
			</ItemDescription>
		{/if}
	</ItemContent>

	<ItemActions>
		{#if github}
			<form method="POST" action="?/unlink" use:enhance={announceUnlink}>
				<input type="hidden" name="accountId" value={github.id} />
				<Button type="submit" variant="outline" size="sm" disabled={!canUnlink}>Disconnect</Button>
			</form>
		{:else}
			<form method="POST" action="?/link">
				<Button type="submit" size="sm">Connect</Button>
			</form>
		{/if}
	</ItemActions>
</Item>
