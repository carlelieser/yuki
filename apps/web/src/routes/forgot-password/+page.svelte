<script lang="ts">
	import { enhance } from '$app/forms';
	import { resolve } from '$app/paths';
	import {
		Button,
		Card,
		CardContent,
		CardDescription,
		CardFooter,
		CardHeader,
		CardTitle,
		Input,
		Label
	} from '@yuki/ui';

	let { form } = $props();
</script>

<svelte:head><title>Reset your password · Yuki</title></svelte:head>

<main class="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-4 py-12">
	<Card>
		{#if form?.sent}
			<CardHeader>
				<CardTitle>Check your email</CardTitle>
				<CardDescription>
					If an account exists for {form.email}, we sent a link to reset its password.
				</CardDescription>
			</CardHeader>
			<CardFooter>
				<a href={resolve('/signin')} class="text-sm underline underline-offset-4">
					Back to sign in
				</a>
			</CardFooter>
		{:else}
			<CardHeader>
				<CardTitle>Reset your password</CardTitle>
				<CardDescription>We'll email you a link to choose a new one.</CardDescription>
			</CardHeader>

			<form method="POST" use:enhance>
				<CardContent class="grid gap-4">
					<div class="grid gap-2">
						<Label for="email">Email</Label>
						<Input
							id="email"
							name="email"
							type="email"
							autocomplete="email"
							required
							aria-invalid={form?.errors?.email ? 'true' : undefined}
							aria-describedby={form?.errors?.email ? 'email-error' : undefined}
						/>
						{#if form?.errors?.email}
							<p id="email-error" class="text-sm text-destructive">{form.errors.email}</p>
						{/if}
					</div>
				</CardContent>

				<CardFooter class="flex flex-col items-stretch gap-3">
					<Button type="submit" class="w-full">Send reset link</Button>
					<p class="text-center text-sm text-muted-foreground">
						<a href={resolve('/signin')} class="underline underline-offset-4">Back to sign in</a>
					</p>
				</CardFooter>
			</form>
		{/if}
	</Card>
</main>
