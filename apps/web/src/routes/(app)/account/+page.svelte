<script lang="ts">
	import { Alert, AlertDescription, AlertTitle } from '@yuki/ui';
	import { Section } from '$lib/components/storefront/index.ts';
	import GithubConnection from '$lib/components/account/github-connection.svelte';
	import type { ActionData, PageData } from './$types';

	let { data, form }: { data: PageData; form: ActionData } = $props();

	const error = $derived(form && 'unlinkError' in form ? form.unlinkError : data.githubError);
</script>

<svelte:head><title>Account · Yuki</title></svelte:head>

<main class="mx-auto w-full max-w-2xl space-y-8 px-4 py-8">
	<h1 class="text-2xl font-semibold tracking-tight">Account</h1>

	{#if error}
		<Alert variant="destructive" role="alert">
			<AlertTitle>GitHub</AlertTitle>
			<AlertDescription>{error}</AlertDescription>
		</Alert>
	{:else if data.isLinked}
		<Alert role="status">
			<AlertTitle>GitHub connected</AlertTitle>
			<AlertDescription>You can now sign in to Yuki with GitHub.</AlertDescription>
		</Alert>
	{/if}

	{#if data.isGithubEnabled || data.github}
		<Section title="Connected accounts">
			<GithubConnection github={data.github} canUnlink={data.canUnlink} />
		</Section>
	{/if}
</main>
