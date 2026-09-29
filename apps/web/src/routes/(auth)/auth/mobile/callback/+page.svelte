<script lang="ts">
	import { Button, Card, CardDescription, CardFooter, CardHeader, CardTitle } from '@yuki/ui';
	import { authCardClass } from '$lib/auth-card.ts';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();
</script>

<svelte:head>
	<title>Return to Yuki</title>
	<meta name="referrer" content="no-referrer" />
</svelte:head>

<main class="w-full">
	<Card class={authCardClass}>
		<CardHeader class="px-0">
			<CardTitle class="text-2xl">Return to the Yuki app</CardTitle>
			<CardDescription>Your GitHub sign-in is ready to finish in the app.</CardDescription>
		</CardHeader>
		<CardFooter class="px-0">
			<Button href={data.appUrl} class="w-full">Open Yuki</Button>
		</CardFooter>
	</Card>
</main>
