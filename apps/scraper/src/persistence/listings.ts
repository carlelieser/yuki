import { and, asc, eq, inArray, sql } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

type Transaction = Parameters<Parameters<Database['transaction']>[0]>[0];
import { scoreConfidence, type DetectedEvidence } from '../detection/evidence.ts';
import type { MappedListing } from '../mapping/listing.ts';
import type { MappedVersion } from '@yuki/github';
import type { ReadmeImage } from '../mapping/readme-images.ts';

export type ListingRecord = {
	id: string;
	owner: string;
	name: string;
	githubRepoId: number;
	packageName: string | null;
};

export type PersistInput = {
	listing: MappedListing | null;
	owner: string;
	name: string;
	githubRepoId?: number;
	iconUrl: string | null;
	bannerUrl: string | null;
	packageName: string | null;
	screenshots: ReadmeImage[] | null;
	versions: MappedVersion[] | null;
	hasApk: boolean | null;
	isAndroidApp: boolean | null;
	evidence: DetectedEvidence[];
};

async function resolveSlug(tx: Transaction, slug: string, githubRepoId: number): Promise<string> {
	const [taken] = await tx
		.select({ githubRepoId: schema.listings.githubRepoId })
		.from(schema.listings)
		.where(eq(schema.listings.slug, slug))
		.limit(1);

	if (taken === undefined || taken.githubRepoId === githubRepoId) return slug;
	return `${slug}-${githubRepoId}`;
}

export async function upsertListing(db: Database, input: PersistInput): Promise<string> {
	return db.transaction(async (tx) => {
		const listingId =
			input.listing === null
				? await updateExisting(tx, input)
				: await insertOrUpdate(tx, input, input.listing);

		if (input.screenshots !== null) {
			await tx
				.delete(schema.listingScreenshots)
				.where(eq(schema.listingScreenshots.listingId, listingId));

			if (input.screenshots.length > 0) {
				await tx.insert(schema.listingScreenshots).values(
					input.screenshots.map((screenshot) => ({
						listingId,
						url: screenshot.url,
						alt: screenshot.alt,
						position: screenshot.position
					}))
				);
			}
		}

		for (const version of input.versions ?? []) {
			await tx
				.insert(schema.listingVersions)
				.values({ listingId, ...version })
				.onConflictDoUpdate({
					target: [schema.listingVersions.listingId, schema.listingVersions.tag],
					set: {
						name: version.name,
						notes: version.notes,
						downloadUrl: version.downloadUrl,
						assetName: version.assetName,
						assetSize: version.assetSize,
						downloadCount: version.downloadCount,
						isPrerelease: version.isPrerelease,
						publishedAt: version.publishedAt,
						updatedAt: new Date()
					}
				});
		}

		for (const entry of input.evidence) {
			await tx
				.insert(schema.listingEvidence)
				.values({ listingId, kind: entry.kind, detail: entry.detail })
				.onConflictDoUpdate({
					target: [schema.listingEvidence.listingId, schema.listingEvidence.kind],
					set: { detail: entry.detail }
				});
		}

		await settleConfidence(tx, listingId);

		return listingId;
	});
}

async function settleConfidence(tx: Transaction, listingId: string): Promise<void> {
	const stored = await tx
		.select({ kind: schema.listingEvidence.kind })
		.from(schema.listingEvidence)
		.where(eq(schema.listingEvidence.listingId, listingId));

	const confidence = scoreConfidence(stored.map((row) => ({ kind: row.kind, detail: null })));

	await tx.update(schema.listings).set({ confidence }).where(eq(schema.listings.id, listingId));
}

async function insertOrUpdate(
	tx: Transaction,
	input: PersistInput,
	listing: MappedListing
): Promise<string> {
	const slug = await resolveSlug(tx, listing.slug, listing.githubRepoId);

	const [row] = await tx
		.insert(schema.listings)
		.values({
			...listing,
			slug,
			iconUrl: input.iconUrl,
			bannerUrl: input.bannerUrl,
			packageName: input.packageName,
			isPublished: input.hasApk === true && scoreConfidence(input.evidence) === 'strong',
			lastScrapedAt: new Date(),
			updatedAt: new Date()
		})
		.onConflictDoUpdate({
			target: schema.listings.githubRepoId,
			set: {
				slug,
				owner: listing.owner,
				name: listing.name,
				title: listing.title,
				author: listing.author,
				authorUrl: listing.authorUrl,
				description: listing.description,
				...(input.iconUrl === null ? {} : { iconUrl: input.iconUrl }),
				...(input.bannerUrl === null ? {} : { bannerUrl: input.bannerUrl }),
				...(input.packageName === null ? {} : { packageName: input.packageName }),
				repositoryUrl: listing.repositoryUrl,
				homepageUrl: listing.homepageUrl,
				license: listing.license,
				stars: listing.stars,
				...(listing.category === null ? {} : { category: listing.category }),
				isFork: listing.isFork,
				isArchived: listing.isArchived,
				repoPushedAt: listing.repoPushedAt,
				lastScrapedAt: new Date(),
				updatedAt: new Date()
			}
		})
		.returning({ id: schema.listings.id });

	if (!row) throw new Error(`Upsert returned no row for ${listing.slug}`);
	return row.id;
}

async function updateExisting(tx: Transaction, input: PersistInput): Promise<string> {
	const [row] = await tx
		.update(schema.listings)
		.set({
			...(input.iconUrl === null ? {} : { iconUrl: input.iconUrl }),
			...(input.bannerUrl === null ? {} : { bannerUrl: input.bannerUrl }),
			...(input.packageName === null ? {} : { packageName: input.packageName }),
			lastScrapedAt: new Date(),
			updatedAt: new Date()
		})
		.where(
			input.githubRepoId === undefined
				? and(eq(schema.listings.owner, input.owner), eq(schema.listings.name, input.name))
				: eq(schema.listings.githubRepoId, input.githubRepoId)
		)
		.returning({ id: schema.listings.id });

	if (!row) throw new Error(`No stored listing for ${input.owner}/${input.name}`);
	return row.id;
}

export async function listListingsForRefresh(
	db: Database,
	limit: number
): Promise<ListingRecord[]> {
	return db
		.select({
			id: schema.listings.id,
			owner: schema.listings.owner,
			name: schema.listings.name,
			githubRepoId: schema.listings.githubRepoId,
			packageName: schema.listings.packageName
		})
		.from(schema.listings)
		.orderBy(sql`${schema.listings.lastScrapedAt} asc nulls first`, asc(schema.listings.id))
		.limit(limit);
}

export async function listListingsBySlug(
	db: Database,
	slugs: string[]
): Promise<(ListingRecord & { slug: string })[]> {
	if (slugs.length === 0) return [];

	return db
		.select({
			id: schema.listings.id,
			slug: schema.listings.slug,
			owner: schema.listings.owner,
			name: schema.listings.name,
			githubRepoId: schema.listings.githubRepoId,
			packageName: schema.listings.packageName
		})
		.from(schema.listings)
		.where(inArray(schema.listings.slug, slugs));
}

export async function listKnownRepoIds(db: Database): Promise<number[]> {
	const rows = await db
		.select({ githubRepoId: schema.listings.githubRepoId })
		.from(schema.listings);

	return rows.map((row) => row.githubRepoId);
}

export async function touchListing(db: Database, listingId: string): Promise<void> {
	await db
		.update(schema.listings)
		.set({ lastScrapedAt: new Date() })
		.where(eq(schema.listings.id, listingId));
}
