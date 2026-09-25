import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { resolveDownload } from '$lib/server/download-target.ts';

export const GET: RequestHandler = async (event) => json({ url: await resolveDownload(event) });
