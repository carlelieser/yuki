import { createDatabase, type Database } from '@yuki/db';

let database: Database | undefined;

export function getDatabase(): Database {
	database ??= createDatabase();
	return database;
}
