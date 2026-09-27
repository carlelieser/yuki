import { describe, expect, it } from 'vitest';
import { QueryBuilder } from 'drizzle-orm/pg-core';
import { schema } from '@yuki/db';
import { publicationOf } from './publication.ts';

describe('publicationOf', () => {
	it('keeps the first publish time when a listing goes live again', () => {
		const { publishedAt } = publicationOf(true);
		if (publishedAt === undefined) throw new Error('expected a publish time');

		const rendered = new QueryBuilder().select({ publishedAt }).from(schema.listings).toSQL().sql;

		expect(rendered).toContain('coalesce("published_at", now())');
	});

	it('leaves the publish time alone when a listing is unpublished', () => {
		expect(publicationOf(false)).toEqual({ isPublished: false });
	});
});
