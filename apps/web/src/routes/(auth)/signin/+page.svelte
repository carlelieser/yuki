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
		CardFooter,
		CardHeader,
		CardTitle,
		Input,
		Label
	} from '@yuki/ui';
	import type { FieldErrors } from '$lib/server/auth-forms.ts';

	let { data, form } = $props();

	const errors: FieldErrors = $derived(form?.errors ?? {});
	const redirectTo = $derived(form?.redirectTo ?? data.redirectTo);
</script>

<svelte:head><title>Sign in · Yuki</title></svelte:head>

<main class="w-full max-w-md">
	<Card>
		<CardHeader>
			<CardTitle>Sign in</CardTitle>
		</CardHeader>

		<form method="POST" use:enhance>
			<input type="hidden" name="redirectTo" value={redirectTo} />

			<CardContent class="grid gap-4">
				{#if form?.message}
					<Alert variant="destructive" role="alert">
						<AlertTitle>Could not sign you in</AlertTitle>
						<AlertDescription>{form.message}</AlertDescription>
					</Alert>
				{/if}

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
					<div class="flex items-center justify-between">
						<Label for="password">Password</Label>
						<a
							href={resolve('/forgot-password')}
							class="text-sm text-muted-foreground underline underline-offset-4"
						>
							Forgot password?
						</a>
					</div>
					<Input
						id="password"
						name="password"
						type="password"
						autocomplete="current-password"
						required
						aria-invalid={errors.password ? 'true' : undefined}
						aria-describedby={errors.password ? 'password-error' : undefined}
					/>
					{#if errors.password}
						<p id="password-error" class="text-sm text-destructive">{errors.password}</p>
					{/if}
				</div>
			</CardContent>

			<CardFooter class="flex flex-col items-stretch gap-3 mt-4">
				<Button type="submit" class="w-full">Sign in</Button>
				<p class="text-center text-sm text-muted-foreground">
					New to Yuki?
					<a href={resolve('/signup')} class="underline underline-offset-4">Create an account</a>
				</p>
			</CardFooter>
		</form>
	</Card>
</main>
