import { relations, sql, type SQL } from 'drizzle-orm';
import {
	boolean,
	customType,
	index,
	integer,
	pgEnum,
	pgTable,
	text,
	timestamp,
	uniqueIndex,
	uuid
} from 'drizzle-orm/pg-core';

const tsvector = customType<{ data: string; driverData: string }>({
	dataType() {
		return 'tsvector';
	}
});

export const listingConfidence = pgEnum('listing_confidence', ['strong', 'probable', 'weak']);

export const listingCategory = pgEnum('listing_category', [
	'system_tweaks',
	'app_management',
	'file_management',
	'media',
	'gaming',
	'automation',
	'networking',
	'privacy_security',
	'developer_tools',
	'device_specific',
	'customization',
	'connectivity',
	'utilities'
]);

export const evidenceKind = pgEnum('evidence_kind', [
	'provider_class',
	'runtime_api_call',
	'gradle_dependency',
	'legacy_gradle_dependency',
	'source_filename',
	'repository_topic',
	'readme_mention'
]);

export const listings = pgTable(
	'listings',
	{
		id: uuid('id').primaryKey().defaultRandom(),
		slug: text('slug').notNull(),
		githubRepoId: integer('github_repo_id').notNull(),
		owner: text('owner').notNull(),
		name: text('name').notNull(),
		title: text('title').notNull(),
		author: text('author').notNull(),
		authorUrl: text('author_url').notNull(),
		description: text('description'),
		iconUrl: text('icon_url'),
		bannerUrl: text('banner_url'),
		packageName: text('package_name'),
		repositoryUrl: text('repository_url').notNull(),
		homepageUrl: text('homepage_url'),
		license: text('license'),
		stars: integer('stars').notNull().default(0),
		confidence: listingConfidence('confidence').notNull(),
		category: listingCategory('category'),
		isFork: boolean('is_fork').notNull().default(false),
		isArchived: boolean('is_archived').notNull().default(false),
		isPublished: boolean('is_published').notNull().default(false),
		repoPushedAt: timestamp('repo_pushed_at', { withTimezone: true }),
		lastScrapedAt: timestamp('last_scraped_at', { withTimezone: true }),
		createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
		updatedAt: timestamp('updated_at', { withTimezone: true }).notNull().defaultNow(),
		searchVector: tsvector('search_vector').generatedAlwaysAs(
			(): SQL =>
				sql`setweight(to_tsvector('english', ${listings.title}), 'A') || setweight(to_tsvector('english', ${listings.name}), 'A') || setweight(to_tsvector('english', ${listings.author}), 'B') || setweight(to_tsvector('english', coalesce(${listings.description}, '')), 'C')`
		)
	},
	(table) => [
		uniqueIndex('listings_github_repo_id_key').on(table.githubRepoId),
		uniqueIndex('listings_slug_key').on(table.slug),
		index('listings_package_name_idx').on(table.packageName),
		index('listings_published_stars_idx').on(table.isPublished, table.stars),
		index('listings_published_category_stars_idx').on(
			table.isPublished,
			table.category,
			table.stars
		),
		index('listings_published_created_idx').on(table.isPublished, table.createdAt),
		index('listings_published_pushed_idx').on(table.isPublished, table.repoPushedAt),
		index('listings_published_title_idx').on(table.isPublished, sql`lower(${table.title})`),
		index('listings_search_vector_idx').using('gin', table.searchVector),
		index('listings_search_title_trgm_idx').using('gin', sql`${table.title} gin_trgm_ops`)
	]
);

export const listingScreenshots = pgTable(
	'listing_screenshots',
	{
		id: uuid('id').primaryKey().defaultRandom(),
		listingId: uuid('listing_id')
			.notNull()
			.references(() => listings.id, { onDelete: 'cascade' }),
		url: text('url').notNull(),
		alt: text('alt'),
		position: integer('position').notNull(),
		createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow()
	},
	(table) => [
		uniqueIndex('listing_screenshots_listing_position_key').on(table.listingId, table.position)
	]
);

export const listingVersions = pgTable(
	'listing_versions',
	{
		id: uuid('id').primaryKey().defaultRandom(),
		listingId: uuid('listing_id')
			.notNull()
			.references(() => listings.id, { onDelete: 'cascade' }),
		tag: text('tag').notNull(),
		name: text('name'),
		notes: text('notes'),
		downloadUrl: text('download_url'),
		assetName: text('asset_name'),
		assetSize: integer('asset_size'),
		downloadCount: integer('download_count').notNull().default(0),
		isPrerelease: boolean('is_prerelease').notNull().default(false),
		publishedAt: timestamp('published_at', { withTimezone: true }),
		createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
		updatedAt: timestamp('updated_at', { withTimezone: true }).notNull().defaultNow()
	},
	(table) => [
		uniqueIndex('listing_versions_listing_tag_key').on(table.listingId, table.tag),
		index('listing_versions_listing_published_idx').on(table.listingId, table.publishedAt)
	]
);

export const listingEvidence = pgTable(
	'listing_evidence',
	{
		id: uuid('id').primaryKey().defaultRandom(),
		listingId: uuid('listing_id')
			.notNull()
			.references(() => listings.id, { onDelete: 'cascade' }),
		kind: evidenceKind('kind').notNull(),
		detail: text('detail'),
		createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow()
	},
	(table) => [uniqueIndex('listing_evidence_listing_kind_key').on(table.listingId, table.kind)]
);

export const listingsRelations = relations(listings, ({ many }) => ({
	screenshots: many(listingScreenshots),
	versions: many(listingVersions),
	evidence: many(listingEvidence)
}));

export const listingScreenshotsRelations = relations(listingScreenshots, ({ one }) => ({
	listing: one(listings, { fields: [listingScreenshots.listingId], references: [listings.id] })
}));

export const listingVersionsRelations = relations(listingVersions, ({ one }) => ({
	listing: one(listings, { fields: [listingVersions.listingId], references: [listings.id] })
}));

export const listingEvidenceRelations = relations(listingEvidence, ({ one }) => ({
	listing: one(listings, { fields: [listingEvidence.listingId], references: [listings.id] })
}));

export type Listing = typeof listings.$inferSelect;
export type NewListing = typeof listings.$inferInsert;
export type ListingScreenshot = typeof listingScreenshots.$inferSelect;
export type NewListingScreenshot = typeof listingScreenshots.$inferInsert;
export type ListingVersion = typeof listingVersions.$inferSelect;
export type NewListingVersion = typeof listingVersions.$inferInsert;
export type ListingEvidence = typeof listingEvidence.$inferSelect;
export type NewListingEvidence = typeof listingEvidence.$inferInsert;
export type ListingConfidence = (typeof listingConfidence.enumValues)[number];
export type ListingCategory = (typeof listingCategory.enumValues)[number];
export type EvidenceKind = (typeof evidenceKind.enumValues)[number];
