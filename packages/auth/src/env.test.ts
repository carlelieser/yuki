import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { getEmailAssetOrigin } from './env.ts';

const originalEnv = { ...process.env };

beforeEach(() => {
	delete process.env.PUBLIC_SITE_URL;
	delete process.env.BETTER_AUTH_URL;
});

afterEach(() => {
	process.env = { ...originalEnv };
});

describe('getEmailAssetOrigin', () => {
	it('uses the public site url, where the logo is actually served from', () => {
		process.env.PUBLIC_SITE_URL = 'https://yukistore.org';

		expect(getEmailAssetOrigin()).toBe('https://yukistore.org');
	});

	it('falls back to the auth url when the site url is unset', () => {
		process.env.BETTER_AUTH_URL = 'https://yukistore.org';

		expect(getEmailAssetOrigin()).toBe('https://yukistore.org');
	});

	it('prefers the site url, since auth may run on a different host', () => {
		process.env.PUBLIC_SITE_URL = 'https://yukistore.org';
		process.env.BETTER_AUTH_URL = 'https://auth.yukistore.org';

		expect(getEmailAssetOrigin()).toBe('https://yukistore.org');
	});

	it('rejects http, because a mail client will not load an insecure image', () => {
		process.env.PUBLIC_SITE_URL = 'http://localhost:5173';

		expect(getEmailAssetOrigin()).toBeUndefined();
	});

	it('returns undefined when nothing is configured', () => {
		expect(getEmailAssetOrigin()).toBeUndefined();
	});

	it('returns undefined for a malformed url instead of building a broken image src', () => {
		process.env.PUBLIC_SITE_URL = 'not-a-url';

		expect(getEmailAssetOrigin()).toBeUndefined();
	});

	it('discards any path, so the logo is not appended to a subdirectory', () => {
		process.env.PUBLIC_SITE_URL = 'https://yukistore.org/app/';

		expect(getEmailAssetOrigin()).toBe('https://yukistore.org');
	});
});
