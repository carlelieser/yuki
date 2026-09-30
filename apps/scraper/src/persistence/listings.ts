import { and, asc, desc, eq, inArray, sql } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import { scoreConfidence, type DetectedEvidence } from '../detection/evidence.ts';
import { settleReleaseDownloads, upsertVersionAssets } from './assets.ts';
import { settleConfidence, shouldPublish, type Transaction } from './publication.ts';
import { settleLatestRelease } from './releases.ts';
import type { MappedListing } from '../mapping/listing.ts';
import type { MappedVersion } from '@yuki/github';
import type { ListingCategory } from '@yuki/db/schema';
import type { ReadmeImage } from '../mapping/readme-images.ts';

export type ListingRecord = {
	id: string;
	owner: string;
	name: string;
	githubRepoId: number;
};

export type PersistInput = {
	listing: MappedListing | null;
	owner: string;
	name: string;
	githubRepoId?: number;
	iconUrl: string | null;
	bannerUrl: string | null;
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
			await upsertVersion(tx, listingId, version);
		}

		if (input.versions !== null) {
			await settleReleaseDownloads(tx, listingId);
			await settleLatestRelease(tx, listingId);
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

async function upsertVersion(
	tx: Transaction,
	listingId: string,
	version: MappedVersion
): Promise<void> {
	const { assets, ...release } = version;
	const [row] = await tx
		.insert(schema.listingVersions)
		.values({ listingId, ...release })
		.onConflictDoUpdate({
			target: [schema.listingVersions.listingId, schema.listingVersions.tag],
			set: {
				name: release.name,
				notes: release.notes,
				downloadUrl: release.downloadUrl,
				assetName: release.assetName,
				assetSize: release.assetSize,
				downloadCount: release.downloadCount,
				isPrerelease: release.isPrerelease,
				publishedAt: release.publishedAt,
				isIgnored: false,
				updatedAt: new Date()
			}
		})
		.returning({ id: schema.listingVersions.id });

	if (!row) throw new Error(`Upsert returned no version for ${listingId} ${release.tag}`);
	await upsertVersionAssets(tx, row.id, assets);
}

async function insertOrUpdate(
	tx: Transaction,
	input: PersistInput,
	listing: MappedListing
): Promise<string> {
	const slug = await resolveSlug(tx, listing.slug, listing.githubRepoId);
	const isPublished = shouldPublish({
		owner: listing.owner,
		name: listing.name,
		confidence: scoreConfidence(input.evidence),
		hasDownloadableAsset: input.hasApk === true
	});

	const [row] = await tx
		.insert(schema.listings)
		.values({
			...listing,
			slug,
			iconUrl: input.iconUrl,
			bannerUrl: input.bannerUrl,
			isPublished,
			publishedAt: isPublished ? new Date() : null,
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
				repositoryUrl: listing.repositoryUrl,
				homepageUrl: listing.homepageUrl,
				license: listing.license,
				stars: listing.stars,
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
	limit?: number
): Promise<ListingRecord[]> {
	const query = db
		.select({
			id: schema.listings.id,
			owner: schema.listings.owner,
			name: schema.listings.name,
			githubRepoId: schema.listings.githubRepoId
		})
		.from(schema.listings)
		.orderBy(sql`${schema.listings.lastScrapedAt} asc nulls first`, asc(schema.listings.id));

	return limit === undefined ? query : query.limit(limit);
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
			githubRepoId: schema.listings.githubRepoId
		})
		.from(schema.listings)
		.where(inArray(schema.listings.slug, slugs));
}

export async function listListingsForReverify(
	db: Database,
	limit: number,
	publishedOnly = false
): Promise<(ListingRecord & { slug: string; stars: number })[]> {
	const query = db
		.select({
			id: schema.listings.id,
			slug: schema.listings.slug,
			owner: schema.listings.owner,
			name: schema.listings.name,
			githubRepoId: schema.listings.githubRepoId,
			stars: schema.listings.stars
		})
		.from(schema.listings);

	return (publishedOnly ? query.where(eq(schema.listings.isPublished, true)) : query)
		.orderBy(desc(schema.listings.stars), asc(schema.listings.id))
		.limit(limit);
}

export type CategorizeTarget = {
	id: string;
	owner: string;
	name: string;
	categoryFingerprint: string | null;
};

export async function listListingsForCategorize(db: Database): Promise<CategorizeTarget[]> {
	return db
		.select({
			id: schema.listings.id,
			owner: schema.listings.owner,
			name: schema.listings.name,
			categoryFingerprint: schema.listings.categoryFingerprint
		})
		.from(schema.listings)
		.where(eq(schema.listings.isPublished, true))
		.orderBy(sql`${schema.listings.categoryFingerprint} is not null`, asc(schema.listings.id));
}

export async function setListingCategory(
	db: Database,
	listingId: string,
	category: ListingCategory | null,
	fingerprint: string
): Promise<void> {
	await db
		.update(schema.listings)
		.set({ category, categoryFingerprint: fingerprint, updatedAt: new Date() })
		.where(eq(schema.listings.id, listingId));
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
