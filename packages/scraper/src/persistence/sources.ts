import { eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';

export async function readEtag(db: Database, resource: string): Promise<string | null> {
	const [row] = await db
		.select({ etag: schema.scrapeSources.etag })
		.from(schema.scrapeSources)
		.where(eq(schema.scrapeSources.resource, resource))
		.limit(1);

	return row?.etag ?? null;
}

export async function writeEtag(
	db: Database,
	resource: string,
	etag: string | null
): Promise<void> {
	await db
		.insert(schema.scrapeSources)
		.values({ resource, etag, fetchedAt: new Date() })
		.onConflictDoUpdate({
			target: schema.scrapeSources.resource,
			set: { etag, fetchedAt: new Date() }
		});
}
