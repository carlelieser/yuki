import { describe, expect, it } from 'vitest';
import { initialsOf } from './initials.ts';

describe('initialsOf', () => {
	it('uses the first letter of the first two words', () => {
		expect(initialsOf('Ada Lovelace')).toBe('AL');
		expect(initialsOf('Grace Brewster Murray Hopper')).toBe('GB');
	});

	it('handles a single word', () => {
		expect(initialsOf('Ada')).toBe('A');
	});

	it('ignores extra whitespace', () => {
		expect(initialsOf('  Ada   Lovelace  ')).toBe('AL');
	});

	it('falls back to the first letter of the fallback when there is no name', () => {
		expect(initialsOf('', 'ada@example.com')).toBe('A');
		expect(initialsOf('   ', 'ada@example.com')).toBe('A');
	});

	it('falls back to a placeholder when there is nothing to abbreviate', () => {
		expect(initialsOf('')).toBe('?');
		expect(initialsOf('   ')).toBe('?');
		expect(initialsOf('', '')).toBe('?');
	});
});
