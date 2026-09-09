import { describe, expect, it } from 'vitest';
import { CATEGORY_OPTIONS, categoryLabel, readCategory } from './categories.ts';

describe('categories', () => {
	it('exposes a label for every enum value', () => {
		expect(CATEGORY_OPTIONS.length).toBeGreaterThan(0);
		for (const option of CATEGORY_OPTIONS) {
			expect(option.label.length).toBeGreaterThan(0);
		}
	});

	it('reads a known category from a query parameter', () => {
		expect(readCategory('gaming')).toBe('gaming');
	});

	it('rejects an unknown or missing category', () => {
		expect(readCategory('not-a-category')).toBeNull();
		expect(readCategory(null)).toBeNull();
	});

	it('labels a category for display', () => {
		expect(categoryLabel('privacy_security')).toBe('Privacy');
	});
});
