<script lang="ts">
	import { Section } from '$lib/components/storefront/index.ts';
	import GithubConnection from '$lib/components/account/github-connection.svelte';
	import PasswordMethod from '$lib/components/account/password-method.svelte';
	import FlashToast from '$lib/components/flash-toast.svelte';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();
</script>

<svelte:head><title>Account · Yuki</title></svelte:head>

<FlashToast title="Could not connect GitHub" description={data.githubError} variant="error" />
<FlashToast
	title="GitHub connected"
	description={data.isLinked ? 'You can now sign in to Yuki with GitHub.' : null}
	variant="success"
/>

<FlashToast
	title="Password set"
	description={data.isPasswordSet ? 'You can now sign in with your email and password.' : null}
	variant="success"
/>

<main class="mx-auto w-full max-w-2xl space-y-8 px-4 py-8">
	<h1 class="text-2xl font-semibold tracking-tight">Account</h1>

	<Section title="Sign-in methods">
		<div class="grid gap-3">
			<PasswordMethod hasPassword={data.hasPassword} email={data.email} />
			{#if data.isGithubEnabled || data.github}
				<GithubConnection github={data.github} canUnlink={data.canUnlink} />
			{/if}
		</div>
	</Section>
</main>
