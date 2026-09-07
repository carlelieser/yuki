import { fail, redirect } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';

const MIN_PASSWORD_LENGTH = 8;

export const load: PageServerLoad = ({ url }) => {
	return { token: url.searchParams.get('token') ?? '' };
};

export const actions: Actions = {
	default: async ({ request }) => {
		const data = await request.formData();
		const token = typeof data.get('token') === 'string' ? String(data.get('token')) : '';
		const password = data.get('password');
		const confirm = data.get('confirmPassword');

		if (!token) {
			return fail(400, {
				message: 'This reset link is invalid or has expired.',
				errors: {}
			});
		}

		if (typeof password !== 'string' || password.length < MIN_PASSWORD_LENGTH) {
			return fail(400, {
				message: '',
				errors: { password: `Password must be at least ${MIN_PASSWORD_LENGTH} characters.` }
			});
		}

		if (password !== confirm) {
			return fail(400, {
				message: '',
				errors: { confirmPassword: 'Passwords do not match.' }
			});
		}

		try {
			await getAuth().api.resetPassword({ body: { token, newPassword: password } });
		} catch (cause) {
			if (cause instanceof APIError) {
				return fail(400, {
					message: 'This reset link is invalid or has expired.',
					errors: {}
				});
			}

			throw cause;
		}

		redirect(303, '/signin');
	}
};
