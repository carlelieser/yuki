import { sequence } from '@sveltejs/kit/hooks';
import type { Handle } from '@sveltejs/kit';
import { getAuth } from '$lib/server/auth.ts';
import { getDatabase } from '$lib/server/database.ts';

const services: Handle = async ({ event, resolve }) => {
	event.locals.db = getDatabase();
	return resolve(event);
};

const session: Handle = async ({ event, resolve }) => {
	const result = await getAuth().api.getSession({ headers: event.request.headers });

	event.locals.user = result?.user ?? null;
	event.locals.session = result?.session ?? null;

	return resolve(event);
};

export const handle = sequence(services, session);
