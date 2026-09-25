import { like } from 'drizzle-orm';
import { createDatabase, schema } from '@yuki/db';
import { requireIconConfig } from './icon/env.ts';
import { ICON_PREFIX, findOrphanedIcons, iconKeyFromUrl } from './icon/prune.ts';
import { createR2Bucket, type StoredObject } from './icon/r2-bucket.ts';

const GRACE_MILLIS = 24 * 60 * 60 * 1000;
const DELETE_CONCURRENCY = 8;

const apply = process.argv.includes('--apply');
const db = createDatabase();
const config = requireIconConfig();
const bucket = createR2Bucket(config.r2);

async function readReferencedKeys(): Promise<Set<string>> {
	const base = config.assetsBaseUrl.replace(/\/+$/, '');
	const rows = await db
		.select({ iconUrl: schema.listings.iconUrl })
		.from(schema.listings)
		.where(like(schema.listings.iconUrl, `${base}/${ICON_PREFIX}%`));

	const keys = rows
		.map((row) => (row.iconUrl === null ? null : iconKeyFromUrl(row.iconUrl, base)))
		.filter((key) => key !== null);
	return new Set(keys);
}

async function removeAll(orphans: StoredObject[]): Promise<void> {
	const queue = [...orphans];

	async function worker(): Promise<void> {
		for (let orphan = queue.shift(); orphan !== undefined; orphan = queue.shift()) {
			await bucket.remove(orphan.key);
		}
	}

	await Promise.all(Array.from({ length: DELETE_CONCURRENCY }, worker));
}

const referenced = await readReferencedKeys();
const stored = await bucket.list(ICON_PREFIX);

if (stored.length > 0 && referenced.size === 0) {
	throw new Error(
		`Refusing to prune: no listing references ${config.assetsBaseUrl}/${ICON_PREFIX}, check ASSETS_BASE_URL`
	);
}

const orphans = findOrphanedIcons(stored, referenced, {
	now: new Date(),
	graceMillis: GRACE_MILLIS
});
console.log(
	`${stored.length} stored icons, ${referenced.size} referenced, ${orphans.length} orphaned for over a day`
);

if (!apply) {
	console.log('\nDry run. Re-run with --apply to delete the orphaned icons.');
	process.exit(0);
}

await removeAll(orphans);
console.log(`Deleted ${orphans.length} orphaned icons`);
process.exit(0);
