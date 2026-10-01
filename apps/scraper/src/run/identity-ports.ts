import type { Database } from '@yuki/db';
import { httpApkSource } from '../apk/http-source.ts';
import { readApkIdentity } from '../apk/identity.ts';
import {
	listPendingIdentities,
	reclassifyAssets,
	saveIdentity,
	settleListings
} from '../persistence/identities.ts';
import type { IdentityPorts } from './identities.ts';

export function createIdentityPorts(
	db: Database,
	resolveListingIds: () => Promise<string[] | undefined>
): IdentityPorts {
	return {
		listPending: async (limit) => listPendingIdentities(db, limit, await resolveListingIds()),
		read: (downloadUrl) => readApkIdentity(httpApkSource(downloadUrl)),
		save: (assetId, identity) => saveIdentity(db, assetId, identity),
		reclassify: () => reclassifyAssets(db),
		settleListings: (listingIds) => settleListings(db, listingIds)
	};
}
