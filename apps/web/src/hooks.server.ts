import type { Handle } from '@sveltejs/kit';
import { getDatabase } from '$lib/server/database.ts';
import { getRedisClient } from '$lib/server/redis.ts';

export const handle: Handle = async ({ event, resolve }) => {
	event.locals.db = getDatabase();
	event.locals.redis = getRedisClient();
	return resolve(event);
};
