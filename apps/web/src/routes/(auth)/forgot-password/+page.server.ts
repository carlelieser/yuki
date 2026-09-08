import { fail } from '@sveltejs/kit';
import { superValidate } from 'sveltekit-superforms';
import { zod4 } from 'sveltekit-superforms/adapters';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { forgotPasswordSchema } from '$lib/schemas/auth.ts';

export const load: PageServerLoad = async () => {
	return { form: await superValidate(zod4(forgotPasswordSchema)) };
};

export const actions: Actions = {
	default: async ({ request }) => {
		const form = await superValidate(request, zod4(forgotPasswordSchema));

		if (!form.valid) return fail(400, { form });

		const { email } = form.data;

		try {
			await getAuth().api.requestPasswordReset({
				body: { email, redirectTo: '/reset-password' },
				headers: request.headers
			});
		} catch {
			return { form, sent: true, email };
		}

		return { form, sent: true, email };
	}
};
