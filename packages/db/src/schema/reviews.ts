import { relations, sql } from 'drizzle-orm';
import {
	check,
	index,
	integer,
	pgTable,
	text,
	timestamp,
	uniqueIndex,
	uuid
} from 'drizzle-orm/pg-core';
import { user } from './auth.ts';
import { listings } from './listings.ts';

export const listingDownloads = pgTable(
	'listing_downloads',
	{
		id: uuid('id').primaryKey().defaultRandom(),
		listingId: uuid('listing_id')
			.notNull()
			.references(() => listings.id, { onDelete: 'cascade' }),
		userId: text('user_id')
			.notNull()
			.references(() => user.id, { onDelete: 'cascade' }),
		versionTag: text('version_tag'),
		createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow()
	},
	(table) => [
		uniqueIndex('listing_downloads_listing_user_key').on(table.listingId, table.userId),
		index('listing_downloads_user_idx').on(table.userId)
	]
);

export const listingReviews = pgTable(
	'listing_reviews',
	{
		id: uuid('id').primaryKey().defaultRandom(),
		listingId: uuid('listing_id')
			.notNull()
			.references(() => listings.id, { onDelete: 'cascade' }),
		userId: text('user_id')
			.notNull()
			.references(() => user.id, { onDelete: 'cascade' }),
		rating: integer('rating').notNull(),
		body: text('body'),
		createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
		updatedAt: timestamp('updated_at', { withTimezone: true }).notNull().defaultNow()
	},
	(table) => [
		uniqueIndex('listing_reviews_listing_user_key').on(table.listingId, table.userId),
		index('listing_reviews_listing_created_idx').on(table.listingId, table.createdAt),
		check('listing_reviews_rating_range', sql`${table.rating} between 1 and 5`)
	]
);

export const listingDownloadsRelations = relations(listingDownloads, ({ one }) => ({
	listing: one(listings, { fields: [listingDownloads.listingId], references: [listings.id] }),
	user: one(user, { fields: [listingDownloads.userId], references: [user.id] })
}));

export const listingReviewsRelations = relations(listingReviews, ({ one }) => ({
	listing: one(listings, { fields: [listingReviews.listingId], references: [listings.id] }),
	user: one(user, { fields: [listingReviews.userId], references: [user.id] })
}));

export type ListingDownload = typeof listingDownloads.$inferSelect;
export type NewListingDownload = typeof listingDownloads.$inferInsert;
export type ListingReview = typeof listingReviews.$inferSelect;
export type NewListingReview = typeof listingReviews.$inferInsert;
