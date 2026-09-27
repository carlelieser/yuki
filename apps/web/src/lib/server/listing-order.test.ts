import { describe, expect, it } from 'vitest';
import { QueryBuilder } from 'drizzle-orm/pg-core';
import { schema } from '@yuki/db';
import type { BrowseOrder, BrowseSort } from '../browse.ts';
import { orderByFor } from './listings.ts';

function renderedOrder(sort: BrowseSort, order: BrowseOrder): string {
	const rendered = new QueryBuilder()
		.select({ id: schema.listings.id })
		.from(schema.listings)
		.orderBy(...orderByFor(sort, order))
		.toSQL().sql;

	return rendered.slice(rendered.indexOf(' order by '));
}

describe('orderByFor', () => {
	it('orders newest listings by when they first went live', () => {
		expect(renderedOrder('newest', 'desc')).toBe(
			' order by "listings"."published_at" desc nulls last, "listings"."id" asc'
		);
	});

	it('orders recently updated listings by their latest stable release', () => {
		expect(renderedOrder('updated', 'desc')).toBe(
			' order by "listings"."latest_release_at" desc nulls last, "listings"."id" asc'
		);
	});
});
