import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { isRedirect } from '@sveltejs/kit';
import type { RequestEvent } from './$types';

const { getAuth, signInSocial } = vi.hoisted(() => {
	const signInSocial = vi.fn();
	return { getAuth: vi.fn(() => ({ api: { signInSocial } })), signInSocial };
});

vi.mock('$lib/server/auth.ts', () => ({ getAuth }));

const { GET } = await import('./+server.ts');

const STATE = 'c'.repeat(43);
const originalEnv = { ...process.env };

function start(state: string) {
	const url = new URL(`https://yukistore.org/auth/mobile/github/start?state=${state}`);
	return { url, request: new Request(url) } as unknown as RequestEvent;
}

async function redirectOf(event: RequestEvent): Promise<URL> {
	try {
		await GET(event);
	} catch (thrown) {
		if (isRedirect(thrown)) return new URL(thrown.location);
		throw thrown;
	}
	return expect.unreachable('expected a redirect');
}

beforeEach(() => {
	process.env.GITHUB_CLIENT_ID = 'client-id';
	process.env.GITHUB_CLIENT_SECRET = 'client-secret';
	signInSocial.mockReset().mockResolvedValue({ url: 'https://github.com/login/oauth/authorize' });
});

afterEach(() => {
	process.env = { ...originalEnv };
});

describe('GET /auth/mobile/github/start', () => {
	it('sends the browser to GitHub and returns through the app hand-off', async () => {
		const target = await redirectOf(start(STATE));

		expect(target.href).toBe('https://github.com/login/oauth/authorize');
		const returnPath = `/auth/mobile/github/signed-in?state=${STATE}`;
		expect(signInSocial).toHaveBeenCalledWith(
			expect.objectContaining({
				body: expect.objectContaining({
					callbackURL: returnPath,
					newUserCallbackURL: returnPath,
					errorCallbackURL: returnPath
				})
			})
		);
	});

	it('refuses a missing or malformed app state', async () => {
		const target = await redirectOf(start('bad'));

		expect(target.pathname).toBe('/auth/mobile/callback');
		expect(target.searchParams.get('error')).toBe('invalid_request');
		expect(signInSocial).not.toHaveBeenCalled();
	});

	it('tells the app when GitHub sign-in is not configured', async () => {
		delete process.env.GITHUB_CLIENT_ID;

		const target = await redirectOf(start(STATE));

		expect(target.searchParams.get('error')).toBe('github_unavailable');
	});
});
