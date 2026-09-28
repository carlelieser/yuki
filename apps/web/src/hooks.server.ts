import { sequence } from '@sveltejs/kit/hooks';
import type { Handle } from '@sveltejs/kit';
import { getAuth } from '$lib/server/auth.ts';
import { getDatabase } from '$lib/server/database.ts';
import { handOffGithubCallback } from '$lib/server/mobile-handoff.ts';

const services: Handle = async ({ event, resolve }) => {
	event.locals.db = getDatabase();
	return resolve(event);
};

const session: Handle = async ({ event, resolve }) => {
	if (event.url.pathname.startsWith('/api/auth/')) return resolve(event);

	const result = await getAuth().api.getSession({ headers: event.request.headers });

	event.locals.user = result?.user ?? null;
	event.locals.session = result?.session ?? null;

	return resolve(event);
};

const githubHandoff: Handle = async ({ event, resolve }) => {
	const response = await resolve(event);
	if (event.url.pathname !== '/api/auth/callback/github') return response;

	return (await handOffGithubCallback(event, response)) ?? response;
};

export const handle = sequence(services, session, githubHandoff);
