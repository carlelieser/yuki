import { redirect } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { resolveDownload } from '$lib/server/download-target.ts';

export const GET: RequestHandler = async (event) => {
	redirect(302, await resolveDownload(event));
};
