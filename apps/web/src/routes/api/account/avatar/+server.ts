import { error, json } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { RequestHandler } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import {
	avatarUrlFor,
	isSupportedAvatarType,
	MAX_AVATAR_BYTES,
	saveAvatar
} from '$lib/server/avatars.ts';

async function readUpload(request: Request): Promise<File> {
	const form = await request.formData().catch(() => null);
	if (form === null) error(400, 'Expected a multipart upload');

	const file = form.get('avatar');
	if (!(file instanceof File)) error(400, 'Expected an avatar file');

	if (!isSupportedAvatarType(file.type)) {
		error(400, `Unsupported image type "${file.type}"`);
	}

	if (file.size > MAX_AVATAR_BYTES) {
		error(400, `The image must be smaller than ${MAX_AVATAR_BYTES} bytes`);
	}

	return file;
}

export const POST: RequestHandler = async ({ locals, request, url }) => {
	if (!locals.user) error(401, 'Sign in to change your picture');

	const file = await readUpload(request);
	const bytes = Buffer.from(await file.arrayBuffer());

	if (bytes.byteLength > MAX_AVATAR_BYTES) {
		error(400, `The image must be smaller than ${MAX_AVATAR_BYTES} bytes`);
	}

	const updatedAt = await saveAvatar(locals.db, {
		userId: locals.user.id,
		contentType: file.type,
		bytes
	});
	const image = avatarUrlFor(locals.user.id, updatedAt, url.origin);

	try {
		await getAuth().api.updateUser({ body: { image }, headers: request.headers });
	} catch (cause) {
		if (cause instanceof APIError) error(400, 'Could not save the picture');
		throw cause;
	}

	return json({ image });
};
