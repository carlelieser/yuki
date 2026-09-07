import type { PageServerLoad } from './$types';

export const load: PageServerLoad = ({ locals, url }) => {
	return {
		verified: locals.user?.emailVerified ?? false,
		error: url.searchParams.get('error')
	};
};
