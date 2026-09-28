import { error, redirect } from '@sveltejs/kit';
import { getGithubCredentials } from '@yuki/auth';
import type { RequestHandler } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { safeRedirectTo } from '$lib/safe-redirect.ts';

export const POST: RequestHandler = async ({ request }) => {
	if (!getGithubCredentials()) error(404, 'GitHub sign-in is not available');

	const form = await request.formData();
	const target = safeRedirectTo(form.get('redirectTo'));
	const errorTarget = `/signin?redirectTo=${encodeURIComponent(target)}`;

	const { url } = await getAuth().api.signInSocial({
		body: {
			provider: 'github',
			callbackURL: target,
			newUserCallbackURL: target,
			errorCallbackURL: errorTarget,
			disableRedirect: true
		},
		headers: request.headers
	});

	if (!url) error(502, 'GitHub sign-in could not start');

	redirect(303, url);
};
