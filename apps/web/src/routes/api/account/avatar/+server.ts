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

type AvatarUpload = {
	contentType: string;
	bytes: Buffer;
};

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

export const POST: RequestHandler = async ({ locals, request, url }) => {
	if (!locals.user) error(401, 'Sign in to change your picture');

	const { contentType, bytes } = await readUpload(request);

	const updatedAt = await saveAvatar(locals.db, {
		userId: locals.user.id,
		contentType,
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
