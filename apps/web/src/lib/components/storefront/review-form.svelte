<script lang="ts">
	import {
		Button,
		FormControl,
		FormField,
		FormFieldErrors,
		FormFieldset,
		StarRatingInput,
		Textarea,
		UserAvatar
	} from '@yuki/ui';
	import { superForm, type SuperValidated } from 'sveltekit-superforms';
	import { zod4Client } from 'sveltekit-superforms/adapters';
	import { reviewSchema, type ReviewInput } from '$lib/schemas/reviews.ts';
	import { initialsOf } from '$lib/initials.ts';
	import { RefreshCcwIcon, SendHorizontalIcon } from '@lucide/svelte';

	let {
		data,
		user,
		isEditing = false
	}: {
		data: SuperValidated<ReviewInput>;
		user: { name: string; email: string; image: string | null };
		isEditing?: boolean;
	} = $props();

	// svelte-ignore state_referenced_locally
	const form = superForm(data, { validators: zod4Client(reviewSchema) });
	const { form: formData, enhance } = form;
</script>

<form method="POST" action="?/review" use:enhance class="space-y-4 rounded-xl border p-4">
	<div class="flex flex-row items-center gap-2">
		<UserAvatar initials={initialsOf(user.name, user.email)} image={user.image} />
		<span class="text-sm font-medium">{user.name}</span>
	</div>

	<div class="flex flex-col gap-2">
		<FormFieldset {form} name="rating">
			<StarRatingInput name="rating" bind:value={$formData.rating} />
			<FormFieldErrors />
		</FormFieldset>

		<FormField {form} name="body">
			{#snippet children({ constraints })}
				<FormControl>
					{#snippet children({ props })}
						<Textarea
							{...props}
							{...constraints}
							rows={4}
							placeholder="Share what worked, and what didn't."
							bind:value={$formData.body}
						/>
					{/snippet}
				</FormControl>
				<FormFieldErrors />
			{/snippet}
		</FormField>
	</div>

	<div class="flex flex-wrap justify-end items-center gap-2">
		{#if isEditing}
			<Button type="submit" formaction="?/deleteReview" variant="ghost">Delete</Button>
			<Button type="submit">
				<RefreshCcwIcon />
				Update
			</Button>
		{:else}
			<Button type="submit">
				<SendHorizontalIcon />
				Post
			</Button>
		{/if}
	</div>
</form>
