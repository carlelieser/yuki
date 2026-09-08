import { error, fail, redirect } from '@sveltejs/kit';
import { message, superValidate } from 'sveltekit-superforms';
import { zod4 } from 'sveltekit-superforms/adapters';
import type { Actions, PageServerLoad } from './$types';
import { getListingBySlug } from '$lib/server/listings.ts';
import {
	deleteReview,
	getRatingSummary,
	getReviewsPage,
	getUserReview,
	hasDownloadedListing,
	upsertReview
} from '$lib/server/reviews.ts';
import { requireUser } from '$lib/server/auth-guard.ts';
import { reviewSchema } from '$lib/schemas/reviews.ts';
import { REVIEWS_PAGE_SIZE } from '$lib/browse.ts';

export const load: PageServerLoad = async ({ locals, params }) => {
	const listing = await getListingBySlug(locals.db, params.slug);
	if (listing === null) error(404, 'Listing not found');

	const user = locals.user;

	const [summary, reviews, existing, canReview] = await Promise.all([
		getRatingSummary(locals.db, listing.id),
		getReviewsPage(locals.db, listing.id, { limit: REVIEWS_PAGE_SIZE, offset: 0 }),
		user ? getUserReview(locals.db, listing.id, user.id) : Promise.resolve(null),
		user ? hasDownloadedListing(locals.db, listing.id, user.id) : Promise.resolve(false)
	]);

	const form = existing
		? await superValidate(
				{ rating: existing.rating, body: existing.body ?? '' },
				zod4(reviewSchema)
			)
		: await superValidate(zod4(reviewSchema));

	return { listing, summary, reviews, hasReviewed: existing !== null, canReview, form };
};

export const actions: Actions = {
	review: async (event) => {
		const user = requireUser(event);
		const form = await superValidate(event.request, zod4(reviewSchema));

		if (!form.valid) return fail(400, { form });

		const listing = await getListingBySlug(event.locals.db, event.params.slug);
		if (listing === null) error(404, 'Listing not found');

		const allowed = await hasDownloadedListing(event.locals.db, listing.id, user.id);
		if (!allowed) {
			return message(form, 'Download this app before reviewing it.', { status: 403 });
		}

		await upsertReview(event.locals.db, {
			listingId: listing.id,
			userId: user.id,
			rating: form.data.rating,
			body: form.data.body === '' ? null : form.data.body
		});

		return message(form, 'Thanks for your review.');
	},

	deleteReview: async (event) => {
		const user = requireUser(event);

		const listing = await getListingBySlug(event.locals.db, event.params.slug);
		if (listing === null) error(404, 'Listing not found');

		await deleteReview(event.locals.db, listing.id, user.id);

		redirect(303, `/listings/${event.params.slug}`);
	}
};
