import { redirect } from '@sveltejs/kit';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { callAuth, forwardCookie, mintTicket, redeemTicket } from '$lib/server/isolated-auth.ts';
import {
	appCallbackUrl,
	isAppState,
	mobileReturnPath,
	LINK_FLOW,
	MOBILE_LINKED_PATH
} from '$lib/server/mobile-handoff.ts';

function appState(value: unknown): string {
	return isAppState(value) ? value : '';
}

export const load = (async (event) => {
	event.setHeaders({ 'cache-control': 'no-store', 'referrer-policy': 'no-referrer' });

	const state = appState(event.url.searchParams.get('state'));
	if (!state) redirect(303, appCallbackUrl(event, { flow: LINK_FLOW, error: 'invalid_request' }));

	const ticket = event.url.searchParams.get('ticket') ?? '';
	const redeemed = ticket ? await redeemTicket(event, ticket) : null;
	const confirmTicket = redeemed ? await mintTicket(event, redeemed.sessionToken) : null;

	if (!redeemed || !confirmTicket) {
		redirect(303, appCallbackUrl(event, { flow: LINK_FLOW, error: 'link_expired', state }));
	}

	return {
		email: redeemed.email,
		confirmTicket,
		state,
		cancelUrl: appCallbackUrl(event, { flow: LINK_FLOW, error: 'access_denied', state })
	};
}) satisfies PageServerLoad;

export const actions = {
	default: async (event) => {
		const form = await event.request.formData();
		const state = appState(form.get('state'));
		if (!state) redirect(303, appCallbackUrl(event, { flow: LINK_FLOW, error: 'invalid_request' }));

		const ticket = form.get('confirmTicket');
		const redeemed = typeof ticket === 'string' ? await redeemTicket(event, ticket) : null;
		if (!redeemed)
			redirect(303, appCallbackUrl(event, { flow: LINK_FLOW, error: 'link_expired', state }));

		const returnPath = mobileReturnPath(MOBILE_LINKED_PATH, state);
		const response = await callAuth(event, '/link-social', {
			method: 'POST',
			sessionToken: redeemed.sessionToken,
			body: {
				provider: 'github',
				callbackURL: returnPath,
				errorCallbackURL: returnPath,
				disableRedirect: true
			}
		});
		const { url } = (await response.json()) as { url?: string };
		if (!response.ok || !url)
			redirect(303, appCallbackUrl(event, { flow: LINK_FLOW, error: 'link_failed', state }));

		const { createAuthCookie } = await getAuth().$context;
		forwardCookie(response, createAuthCookie('state').name, event.cookies);
		redirect(303, url);
	}
} satisfies Actions;
