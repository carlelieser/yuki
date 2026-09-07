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

	let { data } = $props();
</script>

<svelte:head><title>Email verification · Yuki</title></svelte:head>

<main class="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-4 py-12">
	<Card>
		{#if data.verified}
			<CardHeader>
				<CardTitle>Email verified</CardTitle>
				<CardDescription>Your email address is confirmed. You're all set.</CardDescription>
			</CardHeader>
			<CardFooter>
				<a href={resolve('/')} class={buttonVariants({ variant: 'default' })}>Browse apps</a>
			</CardFooter>
		{:else}
			<CardHeader>
				<CardTitle>Verification link expired</CardTitle>
				<CardDescription>
					This link is no longer valid. Sign in again and we'll send you a new one.
				</CardDescription>
			</CardHeader>
			<CardFooter>
				<a href={resolve('/signin')} class={buttonVariants({ variant: 'default' })}>Sign in</a>
			</CardFooter>
		{/if}
	</Card>
</main>
