import { describe, expect, it } from 'vitest';
import { findMissingSlugs, readListFlag } from './args.ts';

describe('readListFlag', () => {
	it('reads a single value', () => {
		expect(readListFlag(['--slug=acme-app'], '--slug')).toEqual(['acme-app']);
	});

	it('splits a comma separated value', () => {
		expect(readListFlag(['--slug=acme-app,acme-tool'], '--slug')).toEqual([
			'acme-app',
			'acme-tool'
		]);
	});

	it('collects a flag that is repeated', () => {
		expect(readListFlag(['--slug=acme-app', '--slug=acme-tool'], '--slug')).toEqual([
			'acme-app',
			'acme-tool'
		]);
	});

	it('trims surrounding whitespace', () => {
		expect(readListFlag(['--slug= acme-app , acme-tool '], '--slug')).toEqual([
			'acme-app',
			'acme-tool'
		]);
	});

	it('drops empty entries left by a trailing comma', () => {
		expect(readListFlag(['--slug=acme-app,'], '--slug')).toEqual(['acme-app']);
	});

	it('returns nothing when the flag is absent', () => {
		expect(readListFlag(['--discover'], '--slug')).toEqual([]);
	});

	it('ignores a flag whose name only shares a prefix', () => {
		expect(readListFlag(['--slugs=acme-app'], '--slug')).toEqual([]);
	});
});

describe('findMissingSlugs', () => {
	it('reports nothing when every slug matched', () => {
		expect(findMissingSlugs(['acme-app'], ['acme-app'])).toEqual([]);
	});

	it('reports the slugs that matched no listing', () => {
		expect(findMissingSlugs(['acme-app', 'ghost'], ['acme-app'])).toEqual(['ghost']);
	});
});
