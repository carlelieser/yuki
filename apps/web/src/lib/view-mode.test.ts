import { describe, expect, it } from 'vitest';
import { parseViewMode } from './view-mode.ts';

describe('parseViewMode', () => {
	it('reads a stored view mode', () => {
		expect(parseViewMode('list')).toBe('list');
	});

	it('falls back to the grid when nothing is stored', () => {
		expect(parseViewMode(null)).toBe('grid');
	});

	it('falls back to the grid when the value is not a view mode', () => {
		expect(parseViewMode('table')).toBe('grid');
	});

	it('ignores surrounding whitespace', () => {
		expect(parseViewMode('  list  ')).toBe('list');
	});
});
