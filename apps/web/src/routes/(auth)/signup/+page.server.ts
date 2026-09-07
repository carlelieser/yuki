import { fail, redirect } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { parseSignUp } from '$lib/server/auth-forms.ts';

export const load: PageServerLoad = ({ locals }) => {
	if (locals.user) {
		redirect(303, '/');
	}
};

export const actions: Actions = {
	default: async ({ request }) => {
		const data = await request.formData();
		const parsed = parseSignUp(data);

		if (!parsed.ok) {
			return fail(400, { email: parsed.email, errors: parsed.errors });
		}

		const { name, email, password } = parsed.value;

		try {
			await getAuth().api.signUpEmail({
				body: { name, email, password },
				headers: request.headers
			});
		} catch (cause) {
			if (cause instanceof APIError) {
				const message =
					cause.body?.code === 'USER_ALREADY_EXISTS'
						? 'An account with that email already exists.'
						: (cause.body?.message ?? 'Could not create your account. Please try again.');

				return fail(400, { email, errors: {}, message });
			}

			throw cause;
		}

		return { email, verificationSent: true };
	}
};
