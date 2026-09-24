import { and, eq } from 'drizzle-orm';
import { createDatabase, schema, type Database } from '@yuki/db';
import {
	getAvatar,
	isSupportedAvatarType,
	removeAvatar,
	uploadAvatar
} from '../src/lib/server/avatars.ts';

type LegacyAvatarOwner = {
	userId: string;
	image: string;
};

const apply = process.argv.includes('--apply');
const db = createDatabase();

function isLegacyAvatarUrl(userId: string, image: string | null): image is string {
	return image?.includes(`/api/users/${userId}/avatar`) ?? false;
}

async function findLegacyAvatarOwners(database: Database): Promise<LegacyAvatarOwner[]> {
	const rows = await database
		.select({ userId: schema.userAvatar.userId, image: schema.user.image })
		.from(schema.userAvatar)
		.innerJoin(schema.user, eq(schema.user.id, schema.userAvatar.userId));

	return rows.filter((row): row is LegacyAvatarOwner => isLegacyAvatarUrl(row.userId, row.image));
}

async function migrateAvatar(database: Database, owner: LegacyAvatarOwner): Promise<boolean> {
	const avatar = await getAvatar(database, owner.userId);
	if (avatar === null) throw new Error(`Avatar for user ${owner.userId} disappeared mid-backfill`);

	const { contentType, bytes } = avatar;
	if (!isSupportedAvatarType(contentType)) {
		throw new Error(`Avatar for user ${owner.userId} has unsupported type "${contentType}"`);
	}

	const image = await uploadAvatar(owner.userId, { contentType, bytes });
	const updated = await database
		.update(schema.user)
		.set({ image, updatedAt: new Date() })
		.where(and(eq(schema.user.id, owner.userId), eq(schema.user.image, owner.image)))
		.returning({ id: schema.user.id });

	if (updated.length > 0) return true;

	await removeAvatar(image);
	return false;
}

const owners = await findLegacyAvatarOwners(db);
console.log(`${owners.length} avatars still served from the database`);

if (!apply) {
	console.log('\nDry run. Re-run with --apply to upload them to object storage.');
	process.exit(0);
}

let migrated = 0;
for (const owner of owners) {
	if (await migrateAvatar(db, owner)) migrated += 1;
}

console.log(`Migrated ${migrated}, skipped ${owners.length - migrated} changed mid-run`);
process.exit(0);
