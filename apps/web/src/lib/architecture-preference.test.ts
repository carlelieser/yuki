import { describe, expect, it } from 'vitest';
import {
	parseArchitecturePreference,
	selectedArchitecture
} from './architecture-preference.ts';

describe('parseArchitecturePreference', () => {
	it('reads a stored architecture', () => {
		expect(parseArchitecturePreference('arm64-v8a')).toBe('arm64-v8a');
	});

	it('has no preference when nothing is stored', () => {
		expect(parseArchitecturePreference(null)).toBeNull();
	});

	it('rejects a value that is not an architecture', () => {
		expect(parseArchitecturePreference('mips')).toBeNull();
		expect(parseArchitecturePreference('')).toBeNull();
	});

	it('ignores surrounding whitespace', () => {
		expect(parseArchitecturePreference('  x86_64  ')).toBe('x86_64');
	});
});

describe('selectedArchitecture', () => {
	it('keeps a preference the release offers', () => {
		expect(selectedArchitecture('arm64-v8a', ['arm64-v8a', 'x86'])).toBe('arm64-v8a');
	});

	it('falls back to the default when the release lacks the preference', () => {
		expect(selectedArchitecture('x86', ['arm64-v8a'])).toBeNull();
	});

	it('falls back to the default before the architectures are known', () => {
		expect(selectedArchitecture('arm64-v8a', [])).toBeNull();
	});

	it('stays on the default when there is no preference', () => {
		expect(selectedArchitecture(null, ['arm64-v8a'])).toBeNull();
	});
});
