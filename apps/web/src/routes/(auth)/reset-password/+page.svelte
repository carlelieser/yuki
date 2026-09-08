<script lang="ts">
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
		FormControl,
		FormField,
		FormFieldErrors,
		FormLabel,
		Input
	} from '@yuki/ui';
	import { superForm } from 'sveltekit-superforms';
	import { zod4Client } from 'sveltekit-superforms/adapters';
	import { resetPasswordSchema } from '$lib/schemas/auth.ts';

	let { data } = $props();

	// svelte-ignore state_referenced_locally
	const form = superForm(data.form, { validators: zod4Client(resetPasswordSchema) });
	const { form: formData, enhance, message } = form;
</script>

<svelte:head><title>Choose a new password · Yuki</title></svelte:head>

<main class="w-full max-w-md">
	<Card>
		<CardHeader>
			<CardTitle>Choose a new password</CardTitle>
			<CardDescription>Pick a password you don't use anywhere else.</CardDescription>
		</CardHeader>

		<form method="POST" use:enhance>
			<input type="hidden" name="token" bind:value={$formData.token} />

			<CardContent class="grid gap-4">
				{#if $message}
					<Alert variant="destructive" role="alert">
						<AlertTitle>Could not reset your password</AlertTitle>
						<AlertDescription>{$message}</AlertDescription>
					</Alert>
				{/if}

				<FormField {form} name="password">
					{#snippet children({ constraints })}
						<FormControl>
							{#snippet children({ props })}
								<FormLabel>New password</FormLabel>
								<Input
									{...props}
									{...constraints}
									type="password"
									autocomplete="new-password"
									bind:value={$formData.password}
								/>
							{/snippet}
						</FormControl>
						<FormFieldErrors />
					{/snippet}
				</FormField>

				<FormField {form} name="confirmPassword">
					{#snippet children({ constraints })}
						<FormControl>
							{#snippet children({ props })}
								<FormLabel>Confirm new password</FormLabel>
								<Input
									{...props}
									{...constraints}
									type="password"
									autocomplete="new-password"
									bind:value={$formData.confirmPassword}
								/>
							{/snippet}
						</FormControl>
						<FormFieldErrors />
					{/snippet}
				</FormField>
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
