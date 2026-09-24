import { randomUUID } from 'node:crypto';
import { eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import { deleteObject, keyFromPublicUrl, publicUrlFor, putObject } from './object-storage.ts';

export const AVATAR_CONTENT_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const;
export const MAX_AVATAR_BYTES = 512 * 1024;

const AVATAR_KEY_PREFIX = 'avatars/';

export type AvatarContentType = (typeof AVATAR_CONTENT_TYPES)[number];

const AVATAR_EXTENSIONS: Record<AvatarContentType, string> = {
	'image/jpeg': 'jpg',
	'image/png': 'png',
	'image/webp': 'webp'
};

export type AvatarUpload = {
	contentType: AvatarContentType;
	bytes: Buffer;
};

export type StoredAvatar = {
	contentType: string;
	bytes: Buffer;
	updatedAt: Date;
};

export function isSupportedAvatarType(contentType: string): contentType is AvatarContentType {
	return (AVATAR_CONTENT_TYPES as readonly string[]).includes(contentType);
}

export async function uploadAvatar(userId: string, upload: AvatarUpload): Promise<string> {
	const extension = AVATAR_EXTENSIONS[upload.contentType];
	const key = `${AVATAR_KEY_PREFIX}${userId}/${randomUUID()}.${extension}`;

	await putObject(key, upload.bytes, upload.contentType);

	return publicUrlFor(key);
}

export async function removeAvatar(image: string | null | undefined): Promise<void> {
	if (!image) return;

	const key = keyFromPublicUrl(image);
	if (key?.startsWith(AVATAR_KEY_PREFIX)) await deleteObject(key);
}

export async function getAvatar(db: Database, userId: string): Promise<StoredAvatar | null> {
	const [row] = await db
		.select({
			contentType: schema.userAvatar.contentType,
			bytes: schema.userAvatar.bytes,
			updatedAt: schema.userAvatar.updatedAt
		})
		.from(schema.userAvatar)
		.where(eq(schema.userAvatar.userId, userId))
		.limit(1);

	return row ?? null;
}
