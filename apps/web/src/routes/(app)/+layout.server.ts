import type { LayoutServerLoad } from './$types';
import { parseArchitecturePreference } from '$lib/architecture-preference.ts';
import { parseViewMode, VIEW_MODE_COOKIE } from '$lib/view-mode.ts';

export const load: LayoutServerLoad = ({ cookies, locals }) => {
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
			: null,
		viewMode: parseViewMode(cookies.get(VIEW_MODE_COOKIE) ?? null)
	};
};
