import { eq } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import { MAPPING_VERSION } from '../mapping/version.ts';

export async function readEtag(db: Database, resource: string): Promise<string | null> {
	const [row] = await db
		.select({
			etag: schema.scrapeSources.etag,
			mappingVersion: schema.scrapeSources.mappingVersion
		})
		.from(schema.scrapeSources)
		.where(eq(schema.scrapeSources.resource, resource))
		.limit(1);

	if (row === undefined || row.mappingVersion !== MAPPING_VERSION) return null;
	return row.etag;
}

export async function writeEtag(
	db: Database,
	resource: string,
	etag: string | null
): Promise<void> {
	await db
		.insert(schema.scrapeSources)
		.values({ resource, etag, mappingVersion: MAPPING_VERSION, fetchedAt: new Date() })
		.onConflictDoUpdate({
			target: schema.scrapeSources.resource,
			set: { etag, mappingVersion: MAPPING_VERSION, fetchedAt: new Date() }
		});
}
