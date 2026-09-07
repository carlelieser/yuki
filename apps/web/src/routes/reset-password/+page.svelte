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

	let { data, form } = $props();

	const errors: FieldErrors = $derived(form?.errors ?? {});
</script>

<svelte:head><title>Choose a new password · Yuki</title></svelte:head>

<main class="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-4 py-12">
	<Card>
		<CardHeader>
			<CardTitle>Choose a new password</CardTitle>
			<CardDescription>Pick a password you don't use anywhere else.</CardDescription>
		</CardHeader>

		<form method="POST" use:enhance>
			<input type="hidden" name="token" value={data.token} />

			<CardContent class="grid gap-4">
				{#if form?.message}
					<Alert variant="destructive" role="alert">
						<AlertTitle>Could not reset your password</AlertTitle>
						<AlertDescription>{form.message}</AlertDescription>
					</Alert>
				{/if}

				<div class="grid gap-2">
					<Label for="password">New password</Label>
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

				<div class="grid gap-2">
					<Label for="confirmPassword">Confirm new password</Label>
					<Input
						id="confirmPassword"
						name="confirmPassword"
						type="password"
						autocomplete="new-password"
						required
						minlength={8}
						aria-invalid={errors.confirmPassword ? 'true' : undefined}
						aria-describedby={errors.confirmPassword ? 'confirm-error' : undefined}
					/>
					{#if errors.confirmPassword}
						<p id="confirm-error" class="text-sm text-destructive">{errors.confirmPassword}</p>
					{/if}
				</div>
			</CardContent>

			<CardFooter class="flex flex-col items-stretch gap-3">
				<Button type="submit" class="w-full">Update password</Button>
				<p class="text-center text-sm text-muted-foreground">
					<a href={resolve('/signin')} class="underline underline-offset-4">Back to sign in</a>
				</p>
			</CardFooter>
		</form>
	</Card>
</main>
