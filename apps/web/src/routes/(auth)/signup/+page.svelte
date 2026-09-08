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
	import { signUpSchema } from '$lib/schemas/auth.ts';

	let { data, form: actionData } = $props();

	// svelte-ignore state_referenced_locally
	const form = superForm(data.form, { validators: zod4Client(signUpSchema) });
	const { form: formData, enhance, message } = form;
</script>

<svelte:head><title>Create an account · Yuki</title></svelte:head>

<main class="w-full max-w-md">
	{#if actionData?.verificationSent}
		<Card>
			<CardHeader>
				<CardTitle>Check your email</CardTitle>
				<CardDescription>
					We sent a verification link to {actionData.email}. Open it to finish setting up your
					account.
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
			</CardHeader>

			<form method="POST" use:enhance>
				<CardContent class="grid gap-4">
					{#if $message}
						<Alert variant="destructive" role="alert">
							<AlertTitle>Could not create your account</AlertTitle>
							<AlertDescription>{$message}</AlertDescription>
						</Alert>
					{/if}

					<FormField {form} name="name">
						{#snippet children({ constraints })}
							<FormControl>
								{#snippet children({ props })}
									<FormLabel>Name</FormLabel>
									<Input
										{...props}
										{...constraints}
										autocomplete="name"
										bind:value={$formData.name}
									/>
								{/snippet}
							</FormControl>
							<FormFieldErrors />
						{/snippet}
					</FormField>

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

					<FormField {form} name="password">
						{#snippet children({ constraints })}
							<FormControl>
								{#snippet children({ props })}
									<FormLabel>Password</FormLabel>
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
				</CardContent>

				<CardFooter class="flex flex-col items-stretch gap-3 mt-4">
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
