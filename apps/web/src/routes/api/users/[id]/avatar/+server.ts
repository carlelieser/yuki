import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { getAvatar } from '$lib/server/avatars.ts';

const CACHE_CONTROL = 'private, max-age=0, must-revalidate';

export const GET: RequestHandler = async ({ locals, params, request }) => {
	const avatar = await getAvatar(locals.db, params.id);
	if (avatar === null) error(404, 'No picture for this account');

	const etag = `"${avatar.updatedAt.getTime()}"`;

	if (request.headers.get('if-none-match') === etag) {
		return new Response(null, {
			status: 304,
			headers: { etag, 'cache-control': CACHE_CONTROL }
		});
	}

	return new Response(new Uint8Array(avatar.bytes), {
		headers: {
			etag,
			'cache-control': CACHE_CONTROL,
			'content-type': avatar.contentType,
			'content-length': String(avatar.bytes.byteLength)
		}
	});
};
