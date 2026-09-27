import { describe, expect, it } from 'vitest';
import { QueryBuilder } from 'drizzle-orm/pg-core';
import { schema } from '@yuki/db';
import { latestStableRelease } from './releases.ts';

function rendered(): string {
	return new QueryBuilder()
		.select({ latestReleaseAt: latestStableRelease })
		.from(schema.listings)
		.toSQL().sql;
}

describe('latestStableRelease', () => {
	it('takes the newest release of the listing being settled', () => {
		expect(rendered()).toContain('select max("published_at")\n\tfrom "listing_versions"');
		expect(rendered()).toContain('"listing_id" = "listings"."id"');
	});

	it('ignores prereleases', () => {
		expect(rendered()).toContain('and not "is_prerelease"');
	});

	it('ignores releases without a downloadable asset', () => {
		expect(rendered()).toContain('and "download_url" is not null');
	});
});
