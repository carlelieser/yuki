import { redirect } from '@sveltejs/kit';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';

export const load: PageServerLoad = () => {
	redirect(303, '/');
};

export const actions: Actions = {
	default: async ({ request }) => {
		await getAuth().api.signOut({ headers: request.headers });
		redirect(303, '/');
	}
};
