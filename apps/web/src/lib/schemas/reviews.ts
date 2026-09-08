import { z } from 'zod';

export const MAX_REVIEW_BODY = 2000;

export const reviewSchema = z.object({
	rating: z.coerce
		.number()
		.int('Choose a rating.')
		.min(1, 'Choose a rating.')
		.max(5, 'Choose a rating.'),
	body: z
		.string()
		.trim()
		.max(MAX_REVIEW_BODY, `Keep your review under ${MAX_REVIEW_BODY} characters.`)
		.default('')
});

export type ReviewInput = z.infer<typeof reviewSchema>;
