import { fail, redirect } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import { message, superValidate } from 'sveltekit-superforms';
import { zod4 } from 'sveltekit-superforms/adapters';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { resetPasswordSchema } from '$lib/schemas/auth.ts';

const SET_MODE = 'set';

export const load = (async ({ url }) => {
	const token = url.searchParams.get('token') ?? '';

	return {
		form: await superValidate({ token }, zod4(resetPasswordSchema), { errors: false }),
		isSettingPassword: url.searchParams.get('mode') === SET_MODE
	};
}) satisfies PageServerLoad;

export const actions = {
	default: async ({ request, url }) => {
		const form = await superValidate(request, zod4(resetPasswordSchema));
		const { token, password } = form.data;

		form.data.password = '';
		form.data.confirmPassword = '';

		if (!form.valid) return fail(400, { form });

		if (!token) {
			return message(form, 'This reset link is invalid or has expired.', { status: 400 });
		}

		try {
			await getAuth().api.resetPassword({ body: { token, newPassword: password } });
		} catch (cause) {
			if (cause instanceof APIError) {
				return message(form, 'This reset link is invalid or has expired.', { status: 400 });
			}

			throw cause;
		}

		redirect(303, url.searchParams.get('mode') === SET_MODE ? '/account?password=set' : '/signin');
	}
} satisfies Actions;
