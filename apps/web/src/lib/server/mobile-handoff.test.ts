import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { RequestEvent } from '@sveltejs/kit';

const { getAuth, handler } = vi.hoisted(() => {
	const handler = vi.fn();
	const context = { authCookies: { sessionToken: { name: 'better-auth.session_token' } } };
	return {
		handler,
		getAuth: vi.fn(() => ({ handler, $context: Promise.resolve(context) }))
	};
});

vi.mock('./auth.ts', () => ({ getAuth }));

const { handOffGithubCallback } = await import('./mobile-handoff.ts');

const STATE = 'a'.repeat(43);

const event = {
	url: new URL('https://yukistore.org/api/auth/callback/github?code=x&state=y'),
	request: new Request('https://yukistore.org/api/auth/callback/github'),
	getClientAddress: () => '203.0.113.9'
} as unknown as RequestEvent;

function callback(location: string, setCookie?: string): Response {
	const headers = new Headers({ location });
	if (setCookie) headers.append('set-cookie', setCookie);
	return new Response(null, { status: 302, headers });
}

function appParams(response: Response | null) {
	const location = new URL(response?.headers.get('location') ?? '');
	expect(location.origin + location.pathname).toBe('https://yukistore.org/auth/mobile/callback');
	return Object.fromEntries(location.searchParams);
}

beforeEach(() => {
	process.env.BETTER_AUTH_URL = 'https://yukistore.org';
	handler.mockReset().mockResolvedValue(Response.json({ token: 'ticket-1' }));
});

describe('handOffGithubCallback', () => {
	it('leaves web sign-ins alone', async () => {
		expect(await handOffGithubCallback(event, callback('/listings/1'))).toBeNull();
	});

	it('hands the new session to the app as a one-time ticket', async () => {
		const response = callback(
			`/auth/mobile/github/signed-in?state=${STATE}`,
			'better-auth.session_token=session-1.sig; Path=/; HttpOnly'
		);

		const handoff = await handOffGithubCallback(event, response);

		expect(appParams(handoff)).toEqual({ token: 'ticket-1', state: STATE });
		const [minted] = handler.mock.calls[0] as [Request];
		expect(minted.url).toBe('https://yukistore.org/api/auth/one-time-token/generate');
		expect(minted.headers.get('authorization')).toBe('Bearer session-1.sig');
	});

	it('never gives the browser the app session', async () => {
		const response = callback(
			`/auth/mobile/github/signed-in?state=${STATE}`,
			'better-auth.session_token=session-1.sig; Path=/; HttpOnly'
		);

		const handoff = await handOffGithubCallback(event, response);

		expect(handoff?.headers.get('set-cookie')).toBeNull();
	});

	it('passes GitHub errors to the app without signing in', async () => {
		const response = callback(
			`/auth/mobile/github/signed-in?state=${STATE}&error=account_not_linked`
		);

		expect(appParams(await handOffGithubCallback(event, response))).toEqual({
			error: 'account_not_linked',
			state: STATE
		});
		expect(handler).not.toHaveBeenCalled();
	});

	it('tells the app when GitHub was linked', async () => {
		const response = callback(`/auth/mobile/github/linked?state=${STATE}`);

		expect(appParams(await handOffGithubCallback(event, response))).toEqual({
			result: 'linked',
			state: STATE
		});
	});

	it('rejects a malformed app state', async () => {
		const response = callback('/auth/mobile/github/signed-in?state=short');

		expect(appParams(await handOffGithubCallback(event, response))).toEqual({
			error: 'invalid_request'
		});
	});

	it('reports a failure when no session was created', async () => {
		const response = callback(`/auth/mobile/github/signed-in?state=${STATE}`);

		expect(appParams(await handOffGithubCallback(event, response)).error).toBe('sign_in_failed');
	});
});
