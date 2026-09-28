import type { Cookies, RequestEvent } from '@sveltejs/kit';
import { requireAuthUrl } from '@yuki/auth';
import { parseSetCookieHeader, toCookieOptions } from 'better-auth/cookies';
import { getAuth } from './auth.ts';

type AuthCall = {
	method: 'GET' | 'POST';
	body?: Record<string, unknown>;
	sessionToken?: string;
};

type RedeemedTicket = { sessionToken: string; email: string };

function clientAddress(event: RequestEvent): string {
	return event.request.headers.get('x-forwarded-for') ?? event.getClientAddress();
}

export function callAuth(event: RequestEvent, path: string, call: AuthCall): Promise<Response> {
	const base = new URL(requireAuthUrl());
	const headers = new Headers({ origin: base.origin, 'x-forwarded-for': clientAddress(event) });

	if (call.body) headers.set('content-type', 'application/json');
	if (call.sessionToken) headers.set('authorization', `Bearer ${call.sessionToken}`);

	const request = new Request(new URL(`/api/auth${path}`, base), {
		method: call.method,
		headers,
		body: call.body ? JSON.stringify(call.body) : undefined
	});

	return getAuth().handler(request);
}

export async function mintTicket(
	event: RequestEvent,
	sessionToken: string
): Promise<string | null> {
	const response = await callAuth(event, '/one-time-token/generate', {
		method: 'GET',
		sessionToken
	});
	if (!response.ok) return null;

	const { token } = (await response.json()) as { token?: string };
	return token ?? null;
}

export async function redeemTicket(
	event: RequestEvent,
	ticket: string
): Promise<RedeemedTicket | null> {
	const response = await callAuth(event, '/one-time-token/verify', {
		method: 'POST',
		body: { token: ticket }
	});
	if (!response.ok) return null;

	const { session, user } = (await response.json()) as {
		session: { token: string };
		user: { email: string };
	};
	return { sessionToken: session.token, email: user.email };
}

export function forwardCookie(response: Response, name: string, cookies: Cookies): void {
	const attributes = parseSetCookieHeader(response.headers.get('set-cookie') ?? '').get(name);
	if (!attributes) return;

	cookies.set(name, attributes.value, {
		...toCookieOptions(attributes),
		path: attributes.path || '/'
	});
}
