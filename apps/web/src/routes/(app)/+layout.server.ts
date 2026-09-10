import type { LayoutServerLoad } from './$types';
import { parseArchitecturePreference } from '$lib/architecture-preference.ts';

export const load: LayoutServerLoad = ({ locals }) => {
	const user = locals.user;

	return {
		user: user
			? {
					id: user.id,
					name: user.name,
					email: user.email,
					image: user.image ?? null,
					architecture: parseArchitecturePreference(user.architecture ?? null)
				}
			: null
	};
};
