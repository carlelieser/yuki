import { toSvelteKitHandler } from 'better-auth/svelte-kit';
import type { RequestHandler } from './$types';
import { getAuth } from '$lib/server/auth.ts';

const handler: RequestHandler = (event) => toSvelteKitHandler(getAuth())(event);

export const GET = handler;
export const POST = handler;
