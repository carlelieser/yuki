<script lang="ts">
	import {
		Button,
		Card,
		CardDescription,
		CardFooter,
		CardHeader,
		CardTitle,
		GithubIcon
	} from '@yuki/ui';
	import { authCardClass } from '$lib/auth-card.ts';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();
</script>

<svelte:head>
	<title>Connect GitHub · Yuki</title>
	<meta name="referrer" content="no-referrer" />
</svelte:head>

<main class="w-full">
	<Card class={authCardClass}>
		<CardHeader class="px-0">
			<CardTitle class="text-2xl">Connect GitHub</CardTitle>
			<CardDescription>
				Connect a GitHub account to the Yuki account {data.email}.
			</CardDescription>
		</CardHeader>

		<form method="POST">
			<input type="hidden" name="state" value={data.state} />
			<input type="hidden" name="confirmTicket" value={data.confirmTicket} />

			<CardFooter class="flex flex-col items-stretch gap-3 px-0">
				<Button type="submit" class="w-full">
					<GithubIcon class="size-4" />
					Continue to GitHub
				</Button>
				<Button href={data.cancelUrl} variant="ghost" class="w-full">Cancel</Button>
			</CardFooter>
		</form>
	</Card>
</main>
