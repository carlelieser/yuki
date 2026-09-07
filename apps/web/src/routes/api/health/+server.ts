import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { checkHealth } from '$lib/server/health.ts';

export const GET: RequestHandler = async ({ locals }) => {
	const report = await checkHealth(locals.db, locals.redis);
	return json(report, { status: report.isHealthy ? 200 : 503 });
};
