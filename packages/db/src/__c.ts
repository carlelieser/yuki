import { Pool } from 'pg';
const pool = new Pool({
	connectionString: process.env.DATABASE_URL,
	connectionTimeoutMillis: 15000
});
try {
	for (const t of [
		'listings',
		'listing_versions',
		'listing_evidence',
		'listing_screenshots',
		'listing_downloads',
		'listing_reviews',
		'scrape_runs',
		'scrape_sources',
		'scrape_partitions',
		'user',
		'session',
		'account'
	]) {
		const r = await pool.query(`select count(*)::int c from "${t}"`).catch(() => null);
		console.log((t + ':').padEnd(22), r ? r.rows[0].c : '(absent)');
	}
} finally {
	await pool.end();
}
