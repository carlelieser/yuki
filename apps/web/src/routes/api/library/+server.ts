import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { findPublishedListingId, getLibrary } from '$lib/server/library.ts';
import { recordDownload } from '$lib/server/reviews.ts';

type AddToLibraryInput = {
	slug: string;
	versionTag: string | null;
};

function readInput(body: unknown): AddToLibraryInput {
	if (body === null || typeof body !== 'object') error(400, 'Expected a JSON body');

	const { slug, versionTag } = body as { slug?: unknown; versionTag?: unknown };

	if (typeof slug !== 'string' || slug.length === 0) error(400, 'Expected a slug');
	if (versionTag !== undefined && versionTag !== null && typeof versionTag !== 'string') {
		error(400, 'Expected a version tag');
	}

	return { slug, versionTag: versionTag ?? null };
}

export const GET: RequestHandler = async ({ locals }) => {
	if (!locals.user) error(401, 'Sign in to see your library');

	return json({ results: await getLibrary(locals.db, locals.user.id) });
};

export const POST: RequestHandler = async ({ locals, request }) => {
	if (!locals.user) error(401, 'Sign in to add to your library');

	const input = readInput(await request.json().catch(() => null));
	const listingId = await findPublishedListingId(locals.db, input.slug);

	if (listingId === null) error(404, `Listing not found for slug "${input.slug}"`);

	await recordDownload(locals.db, {
		listingId,
		userId: locals.user.id,
		versionTag: input.versionTag
	});

	return json({ slug: input.slug });
};
