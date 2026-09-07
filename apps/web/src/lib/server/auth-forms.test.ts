import { describe, expect, it } from 'vitest';
import { parseSignIn, parseSignUp, safeRedirectTo } from './auth-forms.ts';

function formData(entries: Record<string, string>): FormData {
	const data = new FormData();
	for (const [key, value] of Object.entries(entries)) {
		data.set(key, value);
	}
	return data;
}

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

describe('parseSignUp', () => {
	it('accepts a complete submission', () => {
		const result = parseSignUp(
			formData({ name: 'Ada', email: 'ada@example.com', password: 'correct horse' })
		);

		expect(result).toEqual({
			ok: true,
			value: { name: 'Ada', email: 'ada@example.com', password: 'correct horse' }
		});
	});

	it('never echoes the submitted password back on failure', () => {
		const result = parseSignUp(
			formData({ name: '', email: 'not-an-email', password: 'hunter2-secret' })
		);

		expect(result.ok).toBe(false);
		expect(JSON.stringify(result)).not.toContain('hunter2-secret');
	});

	it('rejects short passwords', () => {
		const result = parseSignUp(
			formData({ name: 'Ada', email: 'ada@example.com', password: 'short' })
		);

		expect(result.ok).toBe(false);
		if (!result.ok) {
			expect(result.errors.password).toContain('at least 8');
			expect(result.email).toBe('ada@example.com');
		}
	});
});

describe('parseSignIn', () => {
	it('accepts an email and password pair', () => {
		const result = parseSignIn(formData({ email: 'ada@example.com', password: 'secret-value' }));

		expect(result).toEqual({
			ok: true,
			value: { email: 'ada@example.com', password: 'secret-value' }
		});
	});

	it('reports a missing password without echoing it', () => {
		const result = parseSignIn(formData({ email: 'ada@example.com', password: '' }));

		expect(result.ok).toBe(false);
		if (!result.ok) {
			expect(result.errors.password).toBe('Password is required.');
		}
	});
});
