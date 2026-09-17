import { integer, pgEnum, pgTable, text, timestamp, uniqueIndex, uuid } from 'drizzle-orm/pg-core';

export const scrapeRunStatus = pgEnum('scrape_run_status', ['running', 'succeeded', 'failed']);

export const scrapeSources = pgTable(
	'scrape_sources',
	{
		id: uuid('id').primaryKey().defaultRandom(),
		resource: text('resource').notNull(),
		etag: text('etag'),
		mappingVersion: integer('mapping_version').notNull().default(0),
		fetchedAt: timestamp('fetched_at', { withTimezone: true }).notNull().defaultNow()
	},
	(table) => [uniqueIndex('scrape_sources_resource_key').on(table.resource)]
);

export const scrapePartitions = pgTable(
	'scrape_searched_partitions',
	{
		id: uuid('id').primaryKey().defaultRandom(),
		partition: text('partition').notNull(),
		cursorPage: integer('cursor_page'),
		completedAt: timestamp('completed_at', { withTimezone: true }).notNull().defaultNow()
	},
	(table) => [uniqueIndex('scrape_searched_partitions_key').on(table.partition)]
);

export const scrapeRuns = pgTable('scrape_runs', {
	id: uuid('id').primaryKey().defaultRandom(),
	status: scrapeRunStatus('status').notNull().default('running'),
	discoveredCount: integer('discovered_count').notNull().default(0),
	updatedCount: integer('updated_count').notNull().default(0),
	skippedCount: integer('skipped_count').notNull().default(0),
	requestCount: integer('request_count').notNull().default(0),
	notModifiedCount: integer('not_modified_count').notNull().default(0),
	error: text('error'),
	startedAt: timestamp('started_at', { withTimezone: true }).notNull().defaultNow(),
	finishedAt: timestamp('finished_at', { withTimezone: true })
});

export type ScrapeSource = typeof scrapeSources.$inferSelect;
export type NewScrapeSource = typeof scrapeSources.$inferInsert;
export type ScrapePartition = typeof scrapePartitions.$inferSelect;
export type NewScrapePartition = typeof scrapePartitions.$inferInsert;
export type ScrapeRun = typeof scrapeRuns.$inferSelect;
export type NewScrapeRun = typeof scrapeRuns.$inferInsert;
export type ScrapeRunStatus = (typeof scrapeRunStatus.enumValues)[number];
