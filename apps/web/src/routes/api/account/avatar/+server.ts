import { error, json } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { RequestHandler } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import {
	isSupportedAvatarType,
	MAX_AVATAR_BYTES,
	removeAvatar,
	uploadAvatar,
	type AvatarUpload
} from '$lib/server/avatars.ts';

async function readUpload(request: Request): Promise<AvatarUpload> {
	const payload = await request.json().catch(() => null);
	if (payload === null || typeof payload !== 'object') {
		error(400, 'Expected a JSON body');
	}

	const { contentType, data } = payload as Record<string, unknown>;
	if (typeof contentType !== 'string' || typeof data !== 'string') {
		error(400, 'Expected a contentType and base64 data');
	}

	if (!isSupportedAvatarType(contentType)) {
		error(400, `Unsupported image type "${contentType}"`);
	}

	const bytes = decodeBase64(data);
	if (bytes.byteLength > MAX_AVATAR_BYTES) {
		error(400, `The image must be smaller than ${MAX_AVATAR_BYTES} bytes`);
	}

	if (bytes.byteLength === 0) error(400, 'The image is empty');

	return { contentType, bytes };
}

function decodeBase64(data: string): Buffer {
	const bytes = Buffer.from(data, 'base64');

	if (bytes.toString('base64').replace(/=+$/, '') !== data.replace(/=+$/, '')) {
		error(400, 'The image data is not valid base64');
	}

	return bytes;
}

export const POST: RequestHandler = async ({ locals, request }) => {
	if (!locals.user) error(401, 'Sign in to change your picture');

	const upload = await readUpload(request);
	const image = await uploadAvatar(locals.user.id, upload);

	try {
		await getAuth().api.updateUser({ body: { image }, headers: request.headers });
	} catch (cause) {
		await removeAvatar(image);
		if (cause instanceof APIError) error(400, 'Could not save the picture');
		throw cause;
	}

	await removeAvatar(locals.user.image);

	return json({ image });
};
