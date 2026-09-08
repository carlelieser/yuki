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

	it('falls back when there is nothing to abbreviate', () => {
		expect(initialsOf('')).toBe('?');
		expect(initialsOf('   ')).toBe('?');
	});
});
