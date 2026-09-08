import { fail, redirect } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import { message, superValidate } from 'sveltekit-superforms';
import { zod4 } from 'sveltekit-superforms/adapters';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { signUpSchema } from '$lib/schemas/auth.ts';

export const load: PageServerLoad = async ({ locals }) => {
	if (locals.user) {
		redirect(303, '/');
	}

	return { form: await superValidate(zod4(signUpSchema)) };
};

export const actions: Actions = {
	default: async ({ request }) => {
		const form = await superValidate(request, zod4(signUpSchema));
		const { name, email, password } = form.data;

		form.data.password = '';

		if (!form.valid) return fail(400, { form });

		try {
			await getAuth().api.signUpEmail({
				body: { name, email, password },
				headers: request.headers
			});
		} catch (cause) {
			if (cause instanceof APIError) {
				const text =
					cause.body?.code === 'USER_ALREADY_EXISTS'
						? 'An account with that email already exists.'
						: (cause.body?.message ?? 'Could not create your account. Please try again.');

				return message(form, text, { status: 400 });
			}

			throw cause;
		}

		return { form, verificationSent: true, email };
	}
};
