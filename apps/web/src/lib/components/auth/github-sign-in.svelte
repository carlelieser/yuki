<script lang="ts">
	import { resolve } from '$app/paths';
	import { Alert, AlertDescription, AlertTitle, Button, GithubIcon, Separator } from '@yuki/ui';

	type Props = { error: string | null; redirectTo?: string };

	let { error, redirectTo = '/' }: Props = $props();
</script>

<form method="POST" action={resolve('/auth/github')} class="mb-4 grid gap-4">
	{#if error}
		<Alert variant="destructive" role="alert">
			<AlertTitle>GitHub sign-in failed</AlertTitle>
			<AlertDescription>{error}</AlertDescription>
		</Alert>
	{/if}

	<input type="hidden" name="redirectTo" value={redirectTo} />
	<Button type="submit" variant="outline" class="w-full">
		<GithubIcon class="size-4" />
		Continue with GitHub
	</Button>

	<div class="flex items-center gap-3 text-xs text-muted-foreground">
		<Separator class="flex-1" />
		or
		<Separator class="flex-1" />
	</div>
</form>
