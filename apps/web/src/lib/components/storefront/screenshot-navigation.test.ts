import { describe, expect, it } from 'vitest';
import {
	clampIndex,
	describeScreenshot,
	nextIndex,
	previousIndex
} from './screenshot-navigation.ts';

describe('clampIndex', () => {
	it('keeps an index that is already in range', () => {
		expect(clampIndex(2, 7)).toBe(2);
	});

	it('pulls a negative index up to the first image', () => {
		expect(clampIndex(-3, 7)).toBe(0);
	});

	it('pulls an overflowing index down to the last image', () => {
		expect(clampIndex(99, 7)).toBe(6);
	});

	it('returns zero when there are no images', () => {
		expect(clampIndex(4, 0)).toBe(0);
	});
});

describe('nextIndex', () => {
	it('advances to the following image', () => {
		expect(nextIndex(2, 7)).toBe(3);
	});

	it('wraps to the first image from the last', () => {
		expect(nextIndex(6, 7)).toBe(0);
	});

	it('stays put when there is a single image', () => {
		expect(nextIndex(0, 1)).toBe(0);
	});

	it('returns zero when there are no images', () => {
		expect(nextIndex(0, 0)).toBe(0);
	});

	it('advances from an out-of-range index as if it were clamped', () => {
		expect(nextIndex(-5, 7)).toBe(1);
	});
});

describe('previousIndex', () => {
	it('steps back to the preceding image', () => {
		expect(previousIndex(3, 7)).toBe(2);
	});

	it('wraps to the last image from the first', () => {
		expect(previousIndex(0, 7)).toBe(6);
	});

	it('stays put when there is a single image', () => {
		expect(previousIndex(0, 1)).toBe(0);
	});

	it('returns zero when there are no images', () => {
		expect(previousIndex(0, 0)).toBe(0);
	});
});

describe('describeScreenshot', () => {
	it('uses the provided alt text', () => {
		expect(describeScreenshot('Dashboard view', 0, 7)).toBe('Dashboard view');
	});

	it('falls back to a positional description when alt is missing', () => {
		expect(describeScreenshot(null, 2, 7)).toBe('Screenshot 3 of 7');
	});

	it('keeps an empty alt rather than treating it as decorative', () => {
		expect(describeScreenshot('', 2, 7)).toBe('');
	});
});
