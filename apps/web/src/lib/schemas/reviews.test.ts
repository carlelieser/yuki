import { describe, expect, it } from 'vitest';
import { MAX_REVIEW_BODY, reviewSchema } from './reviews.ts';

describe('reviewSchema', () => {
	it('coerces a rating submitted as a string', () => {
		const result = reviewSchema.safeParse({ rating: '4', body: 'Solid.' });

		expect(result.success).toBe(true);
		expect(result.data?.rating).toBe(4);
	});

	it('accepts a rating without a body', () => {
		const result = reviewSchema.safeParse({ rating: 5, body: '' });

		expect(result.success).toBe(true);
		expect(result.data?.body).toBe('');
	});

	it('rejects ratings outside one through five', () => {
		expect(reviewSchema.safeParse({ rating: 0, body: '' }).success).toBe(false);
		expect(reviewSchema.safeParse({ rating: 6, body: '' }).success).toBe(false);
	});

	it('rejects a fractional rating', () => {
		expect(reviewSchema.safeParse({ rating: 3.5, body: '' }).success).toBe(false);
	});

	it('trims the body and enforces the length ceiling', () => {
		expect(reviewSchema.safeParse({ rating: 3, body: '  spaced  ' }).data?.body).toBe('spaced');
		expect(
			reviewSchema.safeParse({ rating: 3, body: 'x'.repeat(MAX_REVIEW_BODY + 1) }).success
		).toBe(false);
	});
});
