<script lang="ts">
	import { Alert, AlertDescription, AlertTitle, Button, GithubIcon, Separator } from '@yuki/ui';

	type Props = { error: string | null; formId: string };

	let { error, formId }: Props = $props();
</script>

<div class="flex items-center gap-3 text-xs text-muted-foreground">
	<Separator class="flex-1" />
	or
	<Separator class="flex-1" />
</div>

{#if error}
	<Alert variant="destructive" role="alert">
		<AlertTitle>GitHub sign-in failed</AlertTitle>
		<AlertDescription>{error}</AlertDescription>
	</Alert>
{/if}

<Button type="submit" form={formId} variant="outline" class="w-full">
	<GithubIcon class="size-4" />
	Continue with GitHub
</Button>
