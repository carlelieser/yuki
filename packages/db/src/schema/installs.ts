import { relations } from 'drizzle-orm';
import { index, pgTable, text, timestamp, uniqueIndex, uuid } from 'drizzle-orm/pg-core';
import { user } from './auth.ts';
import { listings } from './listings.ts';

export const listingInstalls = pgTable(
	'listing_installs',
	{
		id: uuid('id').primaryKey().defaultRandom(),
		listingId: uuid('listing_id')
			.notNull()
			.references(() => listings.id, { onDelete: 'cascade' }),
		userId: text('user_id')
			.notNull()
			.references(() => user.id, { onDelete: 'cascade' }),
		packageName: text('package_name').notNull(),
		versionTag: text('version_tag'),
		versionCode: text('version_code'),
		installedAt: timestamp('installed_at', { withTimezone: true }).notNull().defaultNow(),
		removedAt: timestamp('removed_at', { withTimezone: true }),
		updatedAt: timestamp('updated_at', { withTimezone: true }).notNull().defaultNow()
	},
	(table) => [
		uniqueIndex('listing_installs_listing_user_key').on(table.listingId, table.userId),
		index('listing_installs_user_active_idx').on(table.userId, table.removedAt),
		index('listing_installs_package_idx').on(table.packageName)
	]
);

export const listingInstallsRelations = relations(listingInstalls, ({ one }) => ({
	listing: one(listings, { fields: [listingInstalls.listingId], references: [listings.id] }),
	user: one(user, { fields: [listingInstalls.userId], references: [user.id] })
}));

export type ListingInstall = typeof listingInstalls.$inferSelect;
export type NewListingInstall = typeof listingInstalls.$inferInsert;
