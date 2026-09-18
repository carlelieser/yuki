import { eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

export const AVATAR_CONTENT_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const;
export const MAX_AVATAR_BYTES = 512 * 1024;

export type StoredAvatar = {
	contentType: string;
	bytes: Buffer;
	updatedAt: Date;
};

export function isSupportedAvatarType(contentType: string): boolean {
	return (AVATAR_CONTENT_TYPES as readonly string[]).includes(contentType);
}

export function avatarUrlFor(userId: string, updatedAt: Date, origin: string): string {
	return new URL(`/api/users/${userId}/avatar?v=${updatedAt.getTime()}`, origin).toString();
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

export async function saveAvatar(
	db: Database,
	input: { userId: string; contentType: string; bytes: Buffer }
): Promise<Date> {
	const updatedAt = new Date();

	await db
		.insert(schema.userAvatar)
		.values({ ...input, updatedAt })
		.onConflictDoUpdate({
			target: schema.userAvatar.userId,
			set: { contentType: input.contentType, bytes: input.bytes, updatedAt }
		});

	return updatedAt;
}
