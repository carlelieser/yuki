import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError, isRedirect } from '@sveltejs/kit';
import type { RequestEvent } from './$types';

const { getAuth, signInSocial } = vi.hoisted(() => {
	const signInSocial = vi.fn();
	return { getAuth: vi.fn(() => ({ api: { signInSocial } })), signInSocial };
});

vi.mock('$lib/server/auth.ts', () => ({ getAuth }));

const { POST } = await import('./+server.ts');

const GITHUB_URL = 'https://github.com/login/oauth/authorize?state=abc';
const originalEnv = { ...process.env };

function submit(redirectTo: string) {
	const body = new URLSearchParams({ redirectTo });
	return {
		request: new Request('http://yuki.test/auth/github', { method: 'POST', body })
	} as unknown as RequestEvent;
}

async function outcome(event: RequestEvent) {
	try {
		await POST(event);
	} catch (thrown) {
		return thrown;
	}
	return expect.unreachable('the handler should redirect or fail');
}

beforeEach(() => {
	process.env.GITHUB_CLIENT_ID = 'client-id';
	process.env.GITHUB_CLIENT_SECRET = 'client-secret';
	signInSocial.mockReset().mockResolvedValue({ url: GITHUB_URL, redirect: false });
});

afterEach(() => {
	process.env = { ...originalEnv };
});

describe('POST /auth/github', () => {
	it('sends the user to GitHub', async () => {
		const result = await outcome(submit('/listings/1'));

		expect(isRedirect(result) && result.location).toBe(GITHUB_URL);
	});

	it('returns to the requested page, and to sign-in on failure', async () => {
		await outcome(submit('/listings/1'));

		expect(signInSocial).toHaveBeenCalledWith(
			expect.objectContaining({
				body: expect.objectContaining({
					provider: 'github',
					callbackURL: '/listings/1',
					newUserCallbackURL: '/listings/1',
					errorCallbackURL: '/signin?redirectTo=%2Flistings%2F1'
				})
			})
		);
	});

	it('ignores redirects to other sites', async () => {
		await outcome(submit('https://evil.test'));

		expect(signInSocial).toHaveBeenCalledWith(
			expect.objectContaining({ body: expect.objectContaining({ callbackURL: '/' }) })
		);
	});

	it('is unavailable when GitHub is not configured', async () => {
		delete process.env.GITHUB_CLIENT_ID;

		const result = await outcome(submit('/'));

		expect(isHttpError(result) && result.status).toBe(404);
		expect(signInSocial).not.toHaveBeenCalled();
	});
});
