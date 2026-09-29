import type { Database } from '@yuki/db';
import { httpApkSource } from '../apk/http-source.ts';
import { readApkIdentity } from '../apk/identity.ts';
import {
	listPendingIdentities,
	saveIdentity,
	settleListingPackageNames
} from '../persistence/identities.ts';
import type { IdentityPorts } from './identities.ts';

export function createIdentityPorts(
	db: Database,
	resolveListingIds: () => Promise<string[] | undefined>
): IdentityPorts {
	return {
		listPending: async (limit) => listPendingIdentities(db, limit, await resolveListingIds()),
		read: (downloadUrl) => readApkIdentity(httpApkSource(downloadUrl)),
		save: (versionId, identity) => saveIdentity(db, versionId, identity),
		settlePackageNames: (listingIds) => settleListingPackageNames(db, listingIds)
	};
}
