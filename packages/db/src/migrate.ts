import { fileURLToPath } from 'node:url';
import { drizzle } from 'drizzle-orm/node-postgres';
import { migrate } from 'drizzle-orm/node-postgres/migrator';
import { Pool } from 'pg';
import { requireDatabaseUrl } from './env.ts';

const migrationsFolder = fileURLToPath(new URL('../drizzle', import.meta.url));
const pool = new Pool({ connectionString: requireDatabaseUrl() });

try {
	await migrate(drizzle(pool), { migrationsFolder });
	console.log('Migrations applied from', migrationsFolder);
} finally {
	await pool.end();
}
