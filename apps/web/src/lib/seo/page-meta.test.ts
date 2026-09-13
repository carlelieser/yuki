import { describe, expect, it } from 'vitest';
import { describeListing, META_DESCRIPTION_LIMIT, truncateForMeta } from './page-meta.ts';
import type { ListingCategory } from '../categories.ts';

type Describable = {
	title: string;
	author: string;
	description: string | null;
	category: ListingCategory | null;
};

function listing(overrides: Partial<Describable> = {}): Describable {
	return {
		title: 'Acme Tools',
		author: 'acme',
		description: null,
		category: 'file_management',
		...overrides
	};
}

describe('truncateForMeta', () => {
	it('leaves a short description untouched', () => {
		expect(truncateForMeta('A small toolbox')).toBe('A small toolbox');
	});

	it('collapses runs of whitespace', () => {
		expect(truncateForMeta('A  small\n\ttoolbox')).toBe('A small toolbox');
	});

	it('cuts on a word boundary rather than mid-word', () => {
		const truncated = truncateForMeta('alpha bravo charlie delta', 18);

		expect(truncated).toBe('alpha bravo…');
		expect(truncated).not.toContain('charl');
	});

	it('stays within the requested limit', () => {
		const truncated = truncateForMeta('x'.repeat(400));

		expect(truncated.length).toBeLessThanOrEqual(META_DESCRIPTION_LIMIT);
	});

	it('drops dangling punctuation before the ellipsis', () => {
		expect(truncateForMeta('alpha bravo, charlie delta', 15)).toBe('alpha bravo…');
	});
});

describe('describeListing', () => {
	it('uses a substantive description as written', () => {
		const description = 'Manage storage, clean junk files, and reclaim space on your device.';

		expect(describeListing(listing({ description }))).toBe(description);
	});

	it('composes a description when the listing has none', () => {
		const composed = describeListing(listing({ description: null }));

		expect(composed).toContain('Acme Tools');
		expect(composed).toContain('acme');
		expect(composed).toContain('files app');
	});

	it('composes a description when the provided one is too thin to be useful', () => {
		const composed = describeListing(listing({ description: 'A toolbox' }));

		expect(composed).not.toBe('A toolbox');
		expect(composed).toContain('Acme Tools');
	});

	it('reads naturally when the listing has no category', () => {
		const composed = describeListing(listing({ description: null, category: null }));

		expect(composed).toContain('open-source app for Android');
		expect(composed).not.toContain('null');
		expect(composed).not.toContain('undefined');
	});

	it('never repeats android when the listing has no category', () => {
		const composed = describeListing(listing({ description: null, category: null }));

		expect(composed.match(/Android/gi)).toHaveLength(1);
	});

	it('names the category in lower case mid-sentence', () => {
		const composed = describeListing(listing({ description: null, category: 'file_management' }));

		expect(composed).toContain('open-source files app for Android');
	});

	it('truncates a long description', () => {
		const composed = describeListing(listing({ description: 'word '.repeat(100) }));

		expect(composed.length).toBeLessThanOrEqual(META_DESCRIPTION_LIMIT);
	});
});
