import type { RequestEvent } from '@sveltejs/kit';
import { parseSetCookieHeader } from 'better-auth/cookies';
import { getAuth } from './auth.ts';
import { mintTicket } from './isolated-auth.ts';

export const MOBILE_SIGNED_IN_PATH = '/auth/mobile/github/signed-in';
export const MOBILE_LINKED_PATH = '/auth/mobile/github/linked';
export const APP_CALLBACK_PATH = '/auth/mobile/callback';

const APP_STATE = /^[A-Za-z0-9_-]{32,128}$/;

export const SIGN_IN_FLOW = 'signin';
export const LINK_FLOW = 'link';

export function isAppState(value: unknown): value is string {
	return typeof value === 'string' && APP_STATE.test(value);
}

export function appCallbackUrl(event: RequestEvent, params: Record<string, string>): string {
	const url = new URL(APP_CALLBACK_PATH, event.url.origin);
	for (const [key, value] of Object.entries(params)) url.searchParams.set(key, value);
	return url.href;
}

export function mobileReturnPath(path: string, state: string): string {
	return `${path}?state=${encodeURIComponent(state)}`;
}

function redirectToApp(event: RequestEvent, params: Record<string, string>): Response {
	return new Response(null, {
		status: 303,
		headers: { location: appCallbackUrl(event, params), 'cache-control': 'no-store' }
	});
}

async function signedInParams(
	event: RequestEvent,
	response: Response,
	state: string
): Promise<Record<string, string>> {
	const { authCookies } = await getAuth().$context;
	const cookies = parseSetCookieHeader(response.headers.get('set-cookie') ?? '');
	const sessionToken = cookies.get(authCookies.sessionToken.name)?.value;
	const ticket = sessionToken ? await mintTicket(event, sessionToken) : null;

	return ticket
		? { flow: SIGN_IN_FLOW, token: ticket, state }
		: { flow: SIGN_IN_FLOW, error: 'sign_in_failed', state };
}

export async function handOffGithubCallback(
	event: RequestEvent,
	response: Response
): Promise<Response | null> {
	const location = response.headers.get('location');
	if (!location) return null;

	const target = new URL(location, event.url);
	const isSignIn = target.pathname === MOBILE_SIGNED_IN_PATH;
	if (!isSignIn && target.pathname !== MOBILE_LINKED_PATH) return null;

	const flow = isSignIn ? SIGN_IN_FLOW : LINK_FLOW;
	const state = target.searchParams.get('state');
	if (!isAppState(state)) return redirectToApp(event, { flow, error: 'invalid_request' });

	const error = target.searchParams.get('error');
	if (error) return redirectToApp(event, { flow, error, state });
	if (!isSignIn) return redirectToApp(event, { flow, result: 'linked', state });

	return redirectToApp(event, await signedInParams(event, response, state));
}
