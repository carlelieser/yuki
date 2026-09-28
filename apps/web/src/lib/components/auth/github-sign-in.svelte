<script lang="ts">
	import { Button, GithubIcon, Separator } from '@yuki/ui';

	type Props = { formId: string };

	let { formId }: Props = $props();
</script>

<div class="flex items-center gap-3 text-xs text-muted-foreground">
	<Separator class="flex-1" />
	or
	<Separator class="flex-1" />
</div>

<Button type="submit" form={formId} variant="outline" class="w-full">
	<GithubIcon class="size-4" />
	Continue with GitHub
</Button>
