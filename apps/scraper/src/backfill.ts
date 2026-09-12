import { createDatabase, schema } from '@yuki/db';
import { eq, inArray } from 'drizzle-orm';
import { isSelfDeclaredOnly, scoreConfidence } from './detection/evidence.ts';
import { resolveTitle } from './mapping/listing.ts';
import type { ListingConfidence } from '@yuki/db/schema';

const AUDITED_NON_APPS = [
	'Xposed-Modules-Repo/com.niki914.zafiro',
	'krishna3163/best_shizuku_apps_for_android_no_root',
	'kerneldroid/Nightzuku-private',
	'lalakii/iceDesk',
	'krishna3163/best-root-apps-for-android',
	'TDA-Android/TDA-Mox',
	'MonDevv-spec/MonProject',
	'nikostap/BlueSpot',
	'KillionWY/KillionFiles',
	'OmerKurdi79/StedNet-Lock-Release',
	'UltimateXDeb/CyberPurge---Kill_Background_Apps_Instantly---With_Shizuku_OR_Assesibilty-',
	'431144610-afk/powerguard',
	'uzvarUA/shaders',
	'arriRgb31/info-layar',
	'ranicola69-cpu/secure-guard-blu-g64'
];

const nonApps = new Set(AUDITED_NON_APPS);
const apply = process.argv.includes('--apply');
const db = createDatabase();

function key(listing: { owner: string; name: string }): string {
	return `${listing.owner}/${listing.name}`;
}

const listings = await db.query.listings.findMany({
	columns: {
		id: true,
		owner: true,
		name: true,
		title: true,
		confidence: true,
		isPublished: true,
		stars: true
	},
	with: { evidence: { columns: { kind: true } } }
});

const regraded = new Map<ListingConfidence, string[]>();
const unpublish: { id: string; label: string; stars: number }[] = [];
const retitled: { id: string; from: string; to: string; stars: number }[] = [];

for (const listing of listings) {
	const evidence = listing.evidence.map((entry) => ({ kind: entry.kind, detail: null }));
	const confidence = scoreConfidence(evidence);

	if (confidence !== listing.confidence) {
		const bucket = regraded.get(confidence) ?? [];
		bucket.push(listing.id);
		regraded.set(confidence, bucket);
	}

	if (listing.isPublished && isSelfDeclaredOnly(evidence) && nonApps.has(key(listing))) {
		unpublish.push({ id: listing.id, label: key(listing), stars: listing.stars });
	}

	const title = resolveTitle(listing.title, listing.name);
	if (title !== listing.title) {
		retitled.push({ id: listing.id, from: listing.title, to: title, stars: listing.stars });
	}
}

console.log(`Scanned ${listings.length} listings.`);
for (const [confidence, ids] of regraded) {
	console.log(`  regrade to ${confidence}: ${ids.length}`);
}

const missing = AUDITED_NON_APPS.filter(
	(label) => !listings.some((listing) => key(listing) === label)
);
if (missing.length > 0) {
	console.log(`  audited entries no longer present: ${missing.join(', ')}`);
}

console.log(`  unpublish: ${unpublish.length}`);
for (const entry of [...unpublish].sort((left, right) => right.stars - left.stars)) {
	console.log(`    ${entry.stars}* ${entry.label}`);
}

console.log(`  retitle: ${retitled.length}`);
for (const entry of [...retitled].sort((left, right) => right.stars - left.stars).slice(0, 20)) {
	console.log(`    ${entry.stars}* ${JSON.stringify(entry.from)} -> ${JSON.stringify(entry.to)}`);
}

if (!apply) {
	console.log('\nDry run. Re-run with --apply to write these changes.');
	process.exit(0);
}

for (const [confidence, ids] of regraded) {
	await db.update(schema.listings).set({ confidence }).where(inArray(schema.listings.id, ids));
}

for (const entry of unpublish) {
	await db
		.update(schema.listings)
		.set({ isPublished: false })
		.where(eq(schema.listings.id, entry.id));
}

for (const entry of retitled) {
	await db.update(schema.listings).set({ title: entry.to }).where(eq(schema.listings.id, entry.id));
}

console.log('\nApplied.');
process.exit(0);
