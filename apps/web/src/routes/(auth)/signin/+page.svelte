<script lang="ts">
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
		FormControl,
		FormField,
		FormFieldErrors,
		FormLabel,
		Input
	} from '@yuki/ui';
	import { superForm } from 'sveltekit-superforms';
	import { zod4Client } from 'sveltekit-superforms/adapters';
	import { signInSchema } from '$lib/schemas/auth.ts';
	import { authCardClass } from '$lib/auth-card.ts';

	let { data } = $props();

	// svelte-ignore state_referenced_locally
	const form = superForm(data.form, { validators: zod4Client(signInSchema) });
	const { form: formData, enhance, message } = form;
</script>

<svelte:head><title>Sign in · Yuki</title></svelte:head>

<main class="w-full">
	<Card class={authCardClass}>
		<CardHeader class="px-0">
			<CardTitle class="text-2xl">Sign in to Yuki</CardTitle>
		</CardHeader>

		<form method="POST" use:enhance>
			<input type="hidden" name="redirectTo" bind:value={$formData.redirectTo} />

			<CardContent class="grid gap-4 px-0">
				{#if $message}
					<Alert variant="destructive" role="alert">
						<AlertTitle>Could not sign you in</AlertTitle>
						<AlertDescription>{$message}</AlertDescription>
					</Alert>
				{/if}

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
								<div class="flex items-center justify-between">
									<FormLabel>Password</FormLabel>
									<a
										href={resolve('/forgot-password')}
										class="text-sm text-muted-foreground underline underline-offset-4"
									>
										Forgot password?
									</a>
								</div>
								<Input
									{...props}
									{...constraints}
									type="password"
									autocomplete="current-password"
									bind:value={$formData.password}
								/>
							{/snippet}
						</FormControl>
						<FormFieldErrors />
					{/snippet}
				</FormField>
			</CardContent>

			<CardFooter class="mt-4 flex flex-col items-stretch gap-3 px-0">
				<Button type="submit" class="w-full">Sign in</Button>
				<p class="text-center text-sm text-muted-foreground">
					New to Yuki?
					<a href={resolve('/signup')} class="underline underline-offset-4">Create an account</a>
				</p>
			</CardFooter>
		</form>
	</Card>
</main>
