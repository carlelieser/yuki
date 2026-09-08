import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { updateCheckSchema } from '$lib/schemas/updates.ts';
import { findAvailableUpdates } from '$lib/server/updates.ts';

export const POST: RequestHandler = async ({ locals, request }) => {
	const payload = await request.json().catch(() => null);
	const parsed = updateCheckSchema.safeParse(payload);

	if (!parsed.success) error(400, 'Invalid update check request');

	const updates = await findAvailableUpdates(locals.db, parsed.data.installed, {
		includePrereleases: parsed.data.includePrereleases
	});

	return json({ updates });
};
