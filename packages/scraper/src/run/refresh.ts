import { mergeEvidence, scoreConfidence, type DetectedEvidence } from '../detection/evidence.ts';
import { GithubSkip, type GithubClient } from '../github/client.ts';
import { buildIconUrl } from '../mapping/icon.ts';
import { mapRepository } from '../mapping/listing.ts';
import { extractReadmeImages } from '../mapping/readme-images.ts';
import { mapReleases } from '../mapping/versions.ts';
import type { GithubRepository, GithubTree } from '../github/types.ts';
import type { PersistInput } from '../persistence/listings.ts';

export type EtagStore = {
	read: (resource: string) => Promise<string | null>;
	write: (resource: string, etag: string | null) => Promise<void>;
};

export type RefreshOutcome =
	{ kind: 'updated'; input: PersistInput } | { kind: 'skipped'; reason: string };

export type RefreshTarget = {
	owner: string;
	name: string;
	evidence?: DetectedEvidence[];
	repo?: GithubRepository;
};

export async function refreshListing(
	client: GithubClient,
	etags: EtagStore,
	target: RefreshTarget
): Promise<RefreshOutcome> {
	const { owner, name } = target;
	const repoResource = `repos/${owner}/${name}`;

	try {
		let repo = target.repo;

		if (repo === undefined) {
			const etag = await etags.read(repoResource);
			const response = await client.getRepository(owner, name, etag);

			if (!response.isModified) {
				return { kind: 'skipped', reason: `${repoResource} not modified` };
			}

			repo = response.body;
			await etags.write(repoResource, response.etag);
		}

		const readme = await readOptional(() => client.getReadme(owner, name));
		const releases = await readOptional(() => client.getReleases(owner, name));
		const tree = await readOptional(() => client.getTree(owner, name, repo.default_branch));

		const evidence = mergeEvidence(target.evidence ?? []);
		const listing = mapRepository(repo, scoreConfidence(evidence), readme);

		return {
			kind: 'updated',
			input: {
				listing,
				iconUrl: iconFrom(tree, owner, name, repo.default_branch),
				screenshots:
					readme === null ? [] : extractReadmeImages(readme, owner, name, repo.default_branch),
				versions: releases === null ? [] : mapReleases(releases),
				evidence
			}
		};
	} catch (cause) {
		if (cause instanceof GithubSkip) {
			return { kind: 'skipped', reason: cause.message };
		}
		throw cause;
	}
}

function iconFrom(
	tree: GithubTree | null,
	owner: string,
	name: string,
	branch: string
): string | null {
	if (tree === null || tree.truncated) return null;
	return buildIconUrl(tree, owner, name, branch);
}

async function readOptional<Body>(
	load: () => Promise<{ isModified: true; body: Body; etag: string | null } | { isModified: false }>
): Promise<Body | null> {
	try {
		const response = await load();
		return response.isModified ? response.body : null;
	} catch (cause) {
		if (cause instanceof GithubSkip) return null;
		throw cause;
	}
}
