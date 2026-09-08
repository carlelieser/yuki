import { fail, redirect } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import { message, superValidate } from 'sveltekit-superforms';
import { zod4 } from 'sveltekit-superforms/adapters';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { safeRedirectTo } from '$lib/safe-redirect.ts';
import { signInSchema } from '$lib/schemas/auth.ts';

export const load: PageServerLoad = async ({ locals, url }) => {
	const redirectTo = safeRedirectTo(url.searchParams.get('redirectTo'));

	if (locals.user) {
		redirect(303, redirectTo as '/');
	}

	return { form: await superValidate({ redirectTo }, zod4(signInSchema), { errors: false }) };
};

export const actions: Actions = {
	default: async ({ request }) => {
		const form = await superValidate(request, zod4(signInSchema));
		const { email, password, redirectTo } = form.data;
		const target = safeRedirectTo(redirectTo);

		form.data.password = '';

		if (!form.valid) return fail(400, { form });

		try {
			await getAuth().api.signInEmail({ body: { email, password }, headers: request.headers });
		} catch (cause) {
			if (cause instanceof APIError) {
				const text =
					cause.body?.code === 'EMAIL_NOT_VERIFIED'
						? 'Verify your email address before signing in. We sent you a new link.'
						: 'Invalid email or password.';

				return message(form, text, { status: 400 });
			}

			throw cause;
		}

		redirect(303, target as '/');
	}
};
