import { fail } from '@sveltejs/kit';
import type { Actions } from './$types';
import { getAuth } from '$lib/server/auth.ts';

export const actions: Actions = {
	default: async ({ request }) => {
		const data = await request.formData();
		const email = typeof data.get('email') === 'string' ? String(data.get('email')).trim() : '';

		if (!email) {
			return fail(400, { errors: { email: 'Email is required.' } });
		}

		try {
			await getAuth().api.requestPasswordReset({
				body: { email, redirectTo: '/reset-password' },
				headers: request.headers
			});
		} catch {
			return { email, sent: true };
		}

		return { email, sent: true };
	}
};
