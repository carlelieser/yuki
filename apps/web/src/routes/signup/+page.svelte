<script lang="ts">
	import { enhance } from '$app/forms';
	import { resolve } from '$app/paths';
	import {
		Alert,
		AlertDescription,
		AlertTitle,
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
	import type { FieldErrors } from '$lib/server/auth-forms.ts';

	let { form } = $props();

	const errors: FieldErrors = $derived(form?.errors ?? {});
</script>

<svelte:head><title>Create an account · Yuki</title></svelte:head>

<main class="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-4 py-12">
	{#if form?.verificationSent}
		<Card>
			<CardHeader>
				<CardTitle>Check your email</CardTitle>
				<CardDescription>
					We sent a verification link to {form.email}. Open it to finish setting up your account.
				</CardDescription>
			</CardHeader>
			<CardFooter>
				<a href={resolve('/signin')} class="text-sm underline underline-offset-4">
					Back to sign in
				</a>
			</CardFooter>
		</Card>
	{:else}
		<Card>
			<CardHeader>
				<CardTitle>Create an account</CardTitle>
				<CardDescription>Sign up to review apps and publish your own.</CardDescription>
			</CardHeader>

			<form method="POST" use:enhance>
				<CardContent class="grid gap-4">
					{#if form?.message}
						<Alert variant="destructive" role="alert">
							<AlertTitle>Could not create your account</AlertTitle>
							<AlertDescription>{form.message}</AlertDescription>
						</Alert>
					{/if}

					<div class="grid gap-2">
						<Label for="name">Name</Label>
						<Input
							id="name"
							name="name"
							autocomplete="name"
							required
							aria-invalid={errors.name ? 'true' : undefined}
							aria-describedby={errors.name ? 'name-error' : undefined}
						/>
						{#if errors.name}
							<p id="name-error" class="text-sm text-destructive">{errors.name}</p>
						{/if}
					</div>

					<div class="grid gap-2">
						<Label for="email">Email</Label>
						<Input
							id="email"
							name="email"
							type="email"
							autocomplete="email"
							required
							value={form?.email ?? ''}
							aria-invalid={errors.email ? 'true' : undefined}
							aria-describedby={errors.email ? 'email-error' : undefined}
						/>
						{#if errors.email}
							<p id="email-error" class="text-sm text-destructive">{errors.email}</p>
						{/if}
					</div>

					<div class="grid gap-2">
						<Label for="password">Password</Label>
						<Input
							id="password"
							name="password"
							type="password"
							autocomplete="new-password"
							required
							minlength={8}
							aria-invalid={errors.password ? 'true' : undefined}
							aria-describedby={errors.password ? 'password-error' : undefined}
						/>
						{#if errors.password}
							<p id="password-error" class="text-sm text-destructive">{errors.password}</p>
						{/if}
					</div>
				</CardContent>

				<CardFooter class="flex flex-col items-stretch gap-3">
					<Button type="submit" class="w-full">Create account</Button>
					<p class="text-center text-sm text-muted-foreground">
						Already have an account?
						<a href={resolve('/signin')} class="underline underline-offset-4">Sign in</a>
					</p>
				</CardFooter>
			</form>
		</Card>
	{/if}
</main>
