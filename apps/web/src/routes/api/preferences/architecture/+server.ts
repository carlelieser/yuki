import { error, json } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { RequestHandler } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { parseArchitecturePreference } from '$lib/architecture-preference.ts';

export const POST: RequestHandler = async ({ locals, request }) => {
	if (!locals.user) error(401, 'Sign in to save an architecture');

	const body: unknown = await request.json().catch(() => null);
	if (body === null || typeof body !== 'object') error(400, 'Expected a JSON body');

	const raw = (body as { architecture?: unknown }).architecture;
	if (raw !== null && typeof raw !== 'string') error(400, 'Expected an architecture');

	const architecture = parseArchitecturePreference(raw);
	if (raw !== null && architecture === null) error(400, `Unknown architecture "${raw}"`);

	try {
		await getAuth().api.updateUser({ body: { architecture }, headers: request.headers });
	} catch (cause) {
		if (cause instanceof APIError) error(400, 'Could not save the architecture');
		throw cause;
	}

	return json({ architecture });
};
