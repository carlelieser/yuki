import { redirect } from '@sveltejs/kit';
import { getGithubCredentials } from '@yuki/auth';
import type { RequestHandler } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import {
	appCallbackUrl,
	isAppState,
	mobileReturnPath,
	MOBILE_SIGNED_IN_PATH,
	SIGN_IN_FLOW
} from '$lib/server/mobile-handoff.ts';

export const GET: RequestHandler = async (event) => {
	const state = event.url.searchParams.get('state');
	if (!isAppState(state))
		redirect(303, appCallbackUrl(event, { flow: SIGN_IN_FLOW, error: 'invalid_request' }));
	if (!getGithubCredentials()) {
		redirect(
			303,
			appCallbackUrl(event, { flow: SIGN_IN_FLOW, error: 'github_unavailable', state })
		);
	}

	const returnPath = mobileReturnPath(MOBILE_SIGNED_IN_PATH, state);
	const { url } = await getAuth().api.signInSocial({
		body: {
			provider: 'github',
			callbackURL: returnPath,
			newUserCallbackURL: returnPath,
			errorCallbackURL: returnPath,
			disableRedirect: true
		},
		headers: event.request.headers
	});

	redirect(
		303,
		url ?? appCallbackUrl(event, { flow: SIGN_IN_FLOW, error: 'sign_in_failed', state })
	);
};
