import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import type { Database } from '@yuki/db';
import { findPublishedListingId } from '$lib/server/library.ts';
import {
	deleteReview,
	getUserReview,
	hasDownloadedListing,
	upsertReview
} from '$lib/server/reviews.ts';
import { reviewSchema, type ReviewInput } from '$lib/schemas/reviews.ts';

async function requireListingId(db: Database, slug: string): Promise<string> {
	const listingId = await findPublishedListingId(db, slug);
	if (listingId === null) error(404, `Listing not found for slug "${slug}"`);

	return listingId;
}

function readInput(body: unknown): ReviewInput {
	const parsed = reviewSchema.safeParse(body);
	if (!parsed.success) error(400, parsed.error.issues[0]?.message ?? 'Expected a review');

	return parsed.data;
}

export const GET: RequestHandler = async ({ locals, params }) => {
	if (!locals.user) error(401, 'Sign in to see your review');

	const listingId = await requireListingId(locals.db, params.slug);
	const [review, canReview] = await Promise.all([
		getUserReview(locals.db, listingId, locals.user.id),
		hasDownloadedListing(locals.db, listingId, locals.user.id)
	]);

	return json({ review, canReview });
};

export const PUT: RequestHandler = async ({ locals, params, request }) => {
	if (!locals.user) error(401, 'Sign in to write a review');

	const input = readInput(await request.json().catch(() => null));
	const listingId = await requireListingId(locals.db, params.slug);

	if (!(await hasDownloadedListing(locals.db, listingId, locals.user.id))) {
		error(403, 'Download this app before reviewing it.');
	}

	await upsertReview(locals.db, {
		listingId,
		userId: locals.user.id,
		rating: input.rating,
		body: input.body === '' ? null : input.body
	});

	return json({ review: await getUserReview(locals.db, listingId, locals.user.id) });
};

export const DELETE: RequestHandler = async ({ locals, params }) => {
	if (!locals.user) error(401, 'Sign in to delete your review');

	const listingId = await requireListingId(locals.db, params.slug);
	await deleteReview(locals.db, listingId, locals.user.id);

	return new Response(null, { status: 204 });
};
