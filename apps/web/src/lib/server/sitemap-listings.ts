import { desc, eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

export type SitemapListing = {
	slug: string;
	repoPushedAt: Date | null;
};

export async function getSitemapListings(db: Database): Promise<SitemapListing[]> {
	return db
		.select({ slug: schema.listings.slug, repoPushedAt: schema.listings.repoPushedAt })
		.from(schema.listings)
		.where(eq(schema.listings.isPublished, true))
		.orderBy(desc(schema.listings.repoPushedAt));
}
