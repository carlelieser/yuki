import { eq, sql } from 'drizzle-orm';
import { schema } from '@yuki/db';
import type { Transaction } from './publication.ts';

export const latestStableRelease = sql<Date | null>`(
	select max(${schema.listingVersions.publishedAt})
	from ${schema.listingVersions}
	where ${schema.listingVersions.listingId} = ${sql`${schema.listings}.${sql.identifier('id')}`}
		and not ${schema.listingVersions.isPrerelease}
		and ${schema.listingVersions.downloadUrl} is not null
)`;

export async function settleLatestRelease(tx: Transaction, listingId: string): Promise<void> {
	await tx
		.update(schema.listings)
		.set({ latestReleaseAt: latestStableRelease })
		.where(eq(schema.listings.id, listingId));
}
