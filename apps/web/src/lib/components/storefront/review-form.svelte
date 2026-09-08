<script lang="ts">
	import {
		Button,
		FormControl,
		FormField,
		FormFieldErrors,
		FormFieldset,
		FormLabel,
		FormLegend,
		StarRatingInput,
		Textarea
	} from '@yuki/ui';
	import { superForm, type SuperValidated } from 'sveltekit-superforms';
	import { zod4Client } from 'sveltekit-superforms/adapters';
	import { reviewSchema, type ReviewInput } from '$lib/schemas/reviews.ts';

	let { data, isEditing = false }: { data: SuperValidated<ReviewInput>; isEditing?: boolean } =
		$props();

	// svelte-ignore state_referenced_locally
	const form = superForm(data, { validators: zod4Client(reviewSchema) });
	const { form: formData, enhance } = form;
</script>

<form method="POST" action="?/review" use:enhance class="space-y-4 rounded-xl border p-4">
	<FormFieldset {form} name="rating" class="space-y-2">
		<FormLegend>Your rating</FormLegend>
		<StarRatingInput name="rating" bind:value={$formData.rating} />
		<FormFieldErrors />
	</FormFieldset>

	<FormField {form} name="body">
		{#snippet children({ constraints })}
			<FormControl>
				{#snippet children({ props })}
					<FormLabel>Your review</FormLabel>
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

	<div class="flex flex-wrap items-center gap-2">
		<Button type="submit">{isEditing ? 'Update review' : 'Post review'}</Button>
		{#if isEditing}
			<Button type="submit" formaction="?/deleteReview" variant="ghost">Delete review</Button>
		{/if}
	</div>
</form>
