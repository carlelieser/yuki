import { fail, redirect } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { parseSignIn, safeRedirectTo } from '$lib/server/auth-forms.ts';

export const load: PageServerLoad = ({ locals, url }) => {
	const redirectTo = safeRedirectTo(url.searchParams.get('redirectTo'));

	if (locals.user) {
		redirect(303, redirectTo as '/');
	}

	return { redirectTo };
};

export const actions: Actions = {
	default: async ({ request }) => {
		const data = await request.formData();
		const redirectTo = safeRedirectTo(data.get('redirectTo'));
		const parsed = parseSignIn(data);

		if (!parsed.ok) {
			return fail(400, { email: parsed.email, errors: parsed.errors, redirectTo });
		}

		const { email, password } = parsed.value;

		try {
			await getAuth().api.signInEmail({ body: { email, password }, headers: request.headers });
		} catch (cause) {
			if (cause instanceof APIError) {
				const message =
					cause.body?.code === 'EMAIL_NOT_VERIFIED'
						? 'Verify your email address before signing in. We sent you a new link.'
						: 'Invalid email or password.';

				return fail(400, { email, errors: {}, message, redirectTo });
			}

			throw cause;
		}

		redirect(303, redirectTo as '/');
	}
};
