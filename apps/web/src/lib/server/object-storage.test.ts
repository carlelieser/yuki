import { beforeEach, describe, expect, it, vi } from 'vitest';
import { keyFromPublicUrl, publicUrlFor } from './object-storage.ts';

beforeEach(() => {
	vi.stubEnv('ASSETS_BASE_URL', 'https://assets.yuki.test/');
});

describe('public asset urls', () => {
	it('serves a key from the assets origin without doubling the slash', () => {
		expect(publicUrlFor('avatars/user-1/a.png')).toBe(
			'https://assets.yuki.test/avatars/user-1/a.png'
		);
	});

	it('recovers the key from a url it built', () => {
		expect(keyFromPublicUrl(publicUrlFor('avatars/user-1/a.png'))).toBe('avatars/user-1/a.png');
	});

	it('does not claim a url hosted elsewhere', () => {
		expect(keyFromPublicUrl('https://avatars.githubusercontent.com/u/1')).toBeNull();
		expect(keyFromPublicUrl('https://assets.yuki.test.evil.example/avatars/a.png')).toBeNull();
	});

	it('does not claim the bare assets origin', () => {
		expect(keyFromPublicUrl('https://assets.yuki.test/')).toBeNull();
	});

	it('names the missing variable when the assets origin is not configured', () => {
		vi.stubEnv('ASSETS_BASE_URL', '');

		expect(() => publicUrlFor('avatars/a.png')).toThrow('ASSETS_BASE_URL');
	});
});
