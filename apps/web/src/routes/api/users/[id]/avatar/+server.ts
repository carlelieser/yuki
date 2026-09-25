import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { findAvatarUrl } from '$lib/server/avatars.ts';

const CACHE_CONTROL = 'private, max-age=0, must-revalidate';

export const GET: RequestHandler = async ({ locals, params }) => {
	const avatarUrl = await findAvatarUrl(locals.db, params.id);
	if (avatarUrl === null) error(404, 'No picture for this account');

	return new Response(null, {
		status: 302,
		headers: { location: avatarUrl, 'cache-control': CACHE_CONTROL }
	});
};
