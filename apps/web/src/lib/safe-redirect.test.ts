import { describe, expect, it } from 'vitest';
import { safeRedirectTo } from './safe-redirect.ts';

describe('safeRedirectTo', () => {
	it('keeps same-origin paths including query strings', () => {
		expect(safeRedirectTo('/reviews/new?draft=1')).toBe('/reviews/new?draft=1');
		expect(safeRedirectTo('/')).toBe('/');
	});

	it('rejects protocol-relative urls that would leave the site', () => {
		expect(safeRedirectTo('//evil.example.com')).toBe('/');
		expect(safeRedirectTo('//evil.example.com/path')).toBe('/');
	});

	it('rejects backslash-prefixed paths browsers may treat as protocol-relative', () => {
		expect(safeRedirectTo('/\\evil.example.com')).toBe('/');
	});

	it('rejects absolute urls', () => {
		expect(safeRedirectTo('https://evil.example.com')).toBe('/');
		expect(safeRedirectTo('http://evil.example.com')).toBe('/');
	});

	it('falls back to the root for missing or non-string values', () => {
		expect(safeRedirectTo(undefined)).toBe('/');
		expect(safeRedirectTo('')).toBe('/');
		expect(safeRedirectTo(null)).toBe('/');
		expect(safeRedirectTo(42)).toBe('/');
	});
});
