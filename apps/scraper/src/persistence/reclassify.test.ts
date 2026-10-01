import { describe, expect, it } from 'vitest';
import { PgDialect } from 'drizzle-orm/pg-core';
import { PLATFORM_CERTIFICATES } from '../apk/publishers.ts';
import { reclassifyQuery } from './identities.ts';

const query = new PgDialect().sqlToQuery(reclassifyQuery());

describe('reclassifyQuery', () => {
	it('marks an apk foreign when every signer is a platform certificate', () => {
		expect(query.sql).toContain('"listing_version_assets"."signer_digests" <@ $1::text[]');
		expect(query.params[0]).toEqual(PLATFORM_CERTIFICATES);
	});

	it('never marks an apk without signers as foreign', () => {
		expect(query.sql).toContain('cardinality("listing_version_assets"."signer_digests") > 0');
	});

	it('classifies an apk whose signers could not be read as its own, not unknown', () => {
		expect(query.sql).toMatch(/= coalesce\(.+, false\)/s);
	});

	it('only touches identified apks whose classification changed', () => {
		expect(query.sql).toContain('"listing_version_assets"."identity_read_at" is not null');
		expect(query.sql).toContain('"listing_version_assets"."is_foreign" is distinct from');
	});

	it('reports the listing of every changed apk', () => {
		expect(query.sql).toMatch(/returning "listing_versions"\."listing_id"/);
	});
});
