import { desc, eq, sql } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import type { EvidenceKind, ListingConfidence } from '@yuki/db/schema';

export type ReviewCandidate = {
	slug: string;
	title: string;
	owner: string;
	name: string;
	description: string | null;
	stars: number;
	confidence: ListingConfidence;
	license: string | null;
	repositoryUrl: string;
	isArchived: boolean;
	isFork: boolean;
	iconUrl: string | null;
	bannerUrl: string | null;
	evidence: { kind: EvidenceKind; detail: string | null }[];
	screenshotCount: number;
	versionCount: number;
};

const confidenceRank = sql`case ${schema.listings.confidence}
	when 'strong' then 0
	when 'probable' then 1
	else 2
end`;

export async function listCandidates(
	db: Database,
	isPublished: boolean,
	limit: number
): Promise<ReviewCandidate[]> {
	const rows = await db.query.listings.findMany({
		where: eq(schema.listings.isPublished, isPublished),
		orderBy: [confidenceRank, desc(schema.listings.stars)],
		limit,
		with: {
			evidence: true,
			screenshots: { columns: { id: true } },
			versions: { columns: { id: true } }
		}
	});

	return rows.map((row) => ({
		slug: row.slug,
		title: row.title,
		owner: row.owner,
		name: row.name,
		description: row.description,
		stars: row.stars,
		confidence: row.confidence,
		license: row.license,
		repositoryUrl: row.repositoryUrl,
		isArchived: row.isArchived,
		isFork: row.isFork,
		iconUrl: row.iconUrl,
		bannerUrl: row.bannerUrl,
		evidence: row.evidence.map((entry) => ({ kind: entry.kind, detail: entry.detail })),
		screenshotCount: row.screenshots.length,
		versionCount: row.versions.length
	}));
}

export async function setPublished(
	db: Database,
	slug: string,
	isPublished: boolean
): Promise<boolean> {
	const updated = await db
		.update(schema.listings)
		.set({ isPublished, updatedAt: new Date() })
		.where(eq(schema.listings.slug, slug))
		.returning({ slug: schema.listings.slug });

	return updated.length > 0;
}
