<script lang="ts">
	import { resolve } from '$app/paths';
	import {
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
	import { forgotPasswordSchema } from '$lib/schemas/auth.ts';

	let { data, form: actionData } = $props();

	// svelte-ignore state_referenced_locally
	const form = superForm(data.form, { validators: zod4Client(forgotPasswordSchema) });
	const { form: formData, enhance } = form;
</script>

<svelte:head><title>Reset your password · Yuki</title></svelte:head>

<main class="w-full max-w-md">
	<Card>
		{#if actionData?.sent}
			<CardHeader>
				<CardTitle>Check your email</CardTitle>
				<CardDescription>
					If an account exists for {actionData.email}, we sent a link to reset its password.
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
					<FormField {form} name="email">
						{#snippet children({ constraints })}
							<FormControl>
								{#snippet children({ props })}
									<FormLabel>Email</FormLabel>
									<Input
										{...props}
										{...constraints}
										type="email"
										autocomplete="email"
										bind:value={$formData.email}
									/>
								{/snippet}
							</FormControl>
							<FormFieldErrors />
						{/snippet}
					</FormField>
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
