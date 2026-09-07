import { drizzle } from 'drizzle-orm/node-postgres';
import { Pool } from 'pg';
import { requireDatabaseUrl } from './env.ts';
import * as schema from './schema/index.ts';

export type Database = ReturnType<typeof createDatabase>;

export function createDatabase(connectionString: string = requireDatabaseUrl()) {
	const pool = new Pool({ connectionString });
	return drizzle(pool, { schema });
}
