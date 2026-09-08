import { error, redirect } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import type { Database } from '@yuki/db';
import { getListingBySlug } from '$lib/server/listings.ts';
import { recordDownload } from '$lib/server/reviews.ts';

async function recordQuietly(
	db: Database,
	input: { listingId: string; userId: string; versionTag: string | null }
): Promise<void> {
	await recordDownload(db, input).catch(() => undefined);
}

export const GET: RequestHandler = async ({ locals, params }) => {
	const listing = await getListingBySlug(locals.db, params.slug);
	if (listing === null) error(404, 'Listing not found');

	const version = listing.versions.find((entry) => entry.tag === params.tag);
	if (!version?.downloadUrl) error(404, 'Download not found');

	if (locals.user) {
		await recordQuietly(locals.db, {
			listingId: listing.id,
			userId: locals.user.id,
			versionTag: version.tag
		});
	}

	redirect(302, version.downloadUrl);
};
