import { describe, expect, it } from 'vitest';
import { drizzle } from 'drizzle-orm/pg-proxy';
import { schema, type Database } from '@yuki/db';
import { getListingBySlug } from './listings.ts';

function capturingDatabase(queries: string[]): Database {
	return drizzle(
		async (sql) => {
			queries.push(sql);
			return { rows: [] };
		},
		{ schema }
	) as unknown as Database;
}

describe('listing releases', () => {
	it('leaves out releases whose apks are all foreign', async () => {
		const queries: string[] = [];

		await getListingBySlug(capturingDatabase(queries), 'ran-mewo-nekoxrmanager');

		expect(queries.join('\n')).toMatch(/not "[a-z_]+"\."is_ignored"/);
	});
});
