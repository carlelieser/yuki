import type { LayoutServerLoad } from './$types';

export const load: LayoutServerLoad = ({ locals }) => {
	const user = locals.user;

	return {
		user: user
			? { id: user.id, name: user.name, email: user.email, image: user.image ?? null }
			: null
	};
};
