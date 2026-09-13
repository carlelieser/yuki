<script lang="ts">
	import { resolve } from '$app/paths';
	import {
		buttonVariants,
		Card,
		CardDescription,
		CardFooter,
		CardHeader,
		CardTitle
	} from '@yuki/ui';
	import { authCardClass } from '$lib/auth-card.ts';

	let { data } = $props();
</script>

<svelte:head><title>Email verification · Yuki</title></svelte:head>

<main class="w-full">
	<Card class={authCardClass}>
		{#if data.verified}
			<CardHeader class="px-0">
				<CardTitle class="text-2xl">Email verified</CardTitle>
				<CardDescription>Your email address is confirmed. You're all set.</CardDescription>
			</CardHeader>
			<CardFooter class="px-0">
				<a href={resolve('/')} class={buttonVariants({ variant: 'default' })}>Browse apps</a>
			</CardFooter>
		{:else}
			<CardHeader class="px-0">
				<CardTitle class="text-2xl">Verification link expired</CardTitle>
				<CardDescription>
					This link is no longer valid. Sign in again and we'll send you a new one.
				</CardDescription>
			</CardHeader>
			<CardFooter class="px-0">
				<a href={resolve('/signin')} class={buttonVariants({ variant: 'default' })}>Sign in</a>
			</CardFooter>
		{/if}
	</Card>
</main>
