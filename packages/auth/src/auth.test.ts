import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { Database } from '@yuki/db';
import { createAuth } from './auth.ts';

const originalEnv = { ...process.env };

beforeEach(() => {
	process.env.BETTER_AUTH_SECRET = 'test-secret-that-is-long-enough-for-better-auth';
	process.env.BETTER_AUTH_URL = 'http://localhost:5173';
	process.env.GITHUB_CLIENT_ID = 'client-id';
	process.env.GITHUB_CLIENT_SECRET = 'client-secret';
});

afterEach(() => {
	process.env = { ...originalEnv };
});

function options() {
	return createAuth({} as Database, () => undefined as never).options;
}

describe('createAuth', () => {
	it('links GitHub by email only when GitHub reports the email as verified', () => {
		expect(options().account?.accountLinking?.trustedProviders).not.toContain('github');
	});

	it('lets a signed-in user connect a GitHub account with a different email', () => {
		expect(options().account?.accountLinking?.allowDifferentEmails).toBe(true);
	});

	it('issues one-time tokens so the Android app can finish browser sign-in', () => {
		expect(options().plugins.map((plugin) => plugin.id)).toContain('one-time-token');
	});

	it('enables GitHub sign-in when credentials are configured', () => {
		expect(options().socialProviders).toHaveProperty('github');
	});
});
