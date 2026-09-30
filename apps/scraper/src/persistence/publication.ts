import { and, eq, isNotNull, sql, type SQL } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import type { ListingConfidence } from '@yuki/db/schema';
import { scoreConfidence, type DetectedEvidence } from '../detection/evidence.ts';

export type Transaction = Parameters<Parameters<Database['transaction']>[0]>[0];

export async function settleConfidence(
	tx: Transaction,
	listingId: string
): Promise<ListingConfidence> {
	const stored = await tx
		.select({ kind: schema.listingEvidence.kind })
		.from(schema.listingEvidence)
		.where(eq(schema.listingEvidence.listingId, listingId));

	const confidence = scoreConfidence(stored.map((row) => ({ kind: row.kind, detail: null })));

	await tx.update(schema.listings).set({ confidence }).where(eq(schema.listings.id, listingId));

	return confidence;
}

const NEVER_PUBLISHED = new Set(['RikkaApps/Shizuku', 'RikkaApps/Sui']);

export function isCatalogueExcluded(owner: string, name: string): boolean {
	return NEVER_PUBLISHED.has(`${owner}/${name}`);
}

export function shouldPublish(input: {
	owner: string;
	name: string;
	confidence: ListingConfidence;
	hasDownloadableAsset: boolean;
}): boolean {
	if (isCatalogueExcluded(input.owner, input.name)) return false;
	return input.hasDownloadableAsset && input.confidence === 'strong';
}

export type Publication = { isPublished: boolean; publishedAt?: SQL };

export function publicationOf(isPublished: boolean): Publication {
	if (!isPublished) return { isPublished };
	return { isPublished, publishedAt: sql`coalesce(${schema.listings.publishedAt}, now())` };
}

export async function hasDistributableRelease(
	tx: Transaction,
	listingId: string
): Promise<boolean> {
	const [release] = await tx
		.select({ id: schema.listingVersions.id })
		.from(schema.listingVersions)
		.where(
			and(
				eq(schema.listingVersions.listingId, listingId),
				isNotNull(schema.listingVersions.downloadUrl)
			)
		)
		.limit(1);

	return release !== undefined;
}

export async function settlePublication(tx: Transaction, listingId: string): Promise<void> {
	const [row] = await tx
		.select({
			isPublished: schema.listings.isPublished,
			owner: schema.listings.owner,
			name: schema.listings.name,
			confidence: schema.listings.confidence
		})
		.from(schema.listings)
		.where(eq(schema.listings.id, listingId))
		.limit(1);
	if (row === undefined) return;

	const isPublished = shouldPublish({
		...row,
		hasDownloadableAsset: await hasDistributableRelease(tx, listingId)
	});
	if (isPublished === row.isPublished) return;

	await tx
		.update(schema.listings)
		.set({ ...publicationOf(isPublished), updatedAt: new Date() })
		.where(eq(schema.listings.id, listingId));
}

export type ReverifyOutcome = {
	confidence: ListingConfidence;
	wasPublished: boolean;
	isPublished: boolean;
};

export async function replaceEvidence(
	db: Database,
	listingId: string,
	evidence: DetectedEvidence[]
): Promise<ReverifyOutcome> {
	return db.transaction(async (tx) => {
		await tx.delete(schema.listingEvidence).where(eq(schema.listingEvidence.listingId, listingId));

		if (evidence.length > 0) {
			await tx
				.insert(schema.listingEvidence)
				.values(evidence.map((entry) => ({ listingId, kind: entry.kind, detail: entry.detail })));
		}

		const confidence = await settleConfidence(tx, listingId);

		const [row] = await tx
			.select({
				isPublished: schema.listings.isPublished,
				owner: schema.listings.owner,
				name: schema.listings.name
			})
			.from(schema.listings)
			.where(eq(schema.listings.id, listingId))
			.limit(1);

		const wasPublished = row?.isPublished === true;
		const isPublished =
			row !== undefined &&
			shouldPublish({
				owner: row.owner,
				name: row.name,
				confidence,
				hasDownloadableAsset: await hasDistributableRelease(tx, listingId)
			});

		if (wasPublished !== isPublished) {
			await tx
				.update(schema.listings)
				.set({ ...publicationOf(isPublished), updatedAt: new Date() })
				.where(eq(schema.listings.id, listingId));
		}

		return { confidence, wasPublished, isPublished };
	});
}
