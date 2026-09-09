import { mergeEvidence, scoreConfidence, type DetectedEvidence } from '../detection/evidence.ts';
import { GithubSkip, type GithubClient } from '../github/client.ts';
import { buildIconUrl, buildVectorIcon } from '../mapping/icon.ts';
import { mapRepository } from '../mapping/listing.ts';
import { extractReadmeImages, findBannerUrl } from '../mapping/readme-images.ts';
import { hasDistributableApk, mapReleases } from '../mapping/versions.ts';
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

type Resource<Body> = { state: 'fresh'; body: Body } | { state: 'unchanged' } | { state: 'absent' };

export async function refreshListing(
	client: GithubClient,
	etags: EtagStore,
	target: RefreshTarget
): Promise<RefreshOutcome> {
	const { owner, name } = target;
	const repoResource = `repos/${owner}/${name}`;

	try {
		const repo = await readRepository(client, etags, target, repoResource);
		if (repo.state === 'absent') {
			return { kind: 'skipped', reason: `${repoResource} not found` };
		}

		const branch = repo.state === 'fresh' ? repo.body.default_branch : repo.branch;

		const releases = await readResource(etags, `${repoResource}/releases`, (etag) =>
			client.getReleases(owner, name, etag)
		);
		const readme = await readResource(etags, `${repoResource}/readme`, (etag) =>
			client.getReadme(owner, name, etag)
		);
		const tree = await readResource(etags, `${repoResource}/git/trees/${branch}`, (etag) =>
			client.getTree(owner, name, branch, etag)
		);

		if (
			repo.state === 'unchanged' &&
			releases.state === 'unchanged' &&
			readme.state === 'unchanged' &&
			tree.state === 'unchanged'
		) {
			return { kind: 'skipped', reason: `${repoResource} not modified` };
		}

		const evidence = mergeEvidence(target.evidence ?? []);
		const versions =
			releases.state === 'unchanged'
				? null
				: releases.state === 'absent'
					? []
					: mapReleases(releases.body);
		const readmeBody = readme.state === 'fresh' ? readme.body : null;
		const bannerUrl = readmeBody === null ? null : findBannerUrl(readmeBody, owner, name, branch);

		return {
			kind: 'updated',
			input: {
				owner,
				name,
				listing:
					repo.state === 'fresh'
						? mapRepository(repo.body, scoreConfidence(evidence), readmeBody)
						: null,
				iconUrl:
					tree.state === 'fresh' ? await iconFrom(client, tree.body, owner, name, branch) : null,
				bannerUrl: readme.state === 'unchanged' ? null : bannerUrl,
				screenshots:
					readme.state === 'unchanged'
						? null
						: readmeBody === null
							? []
							: extractReadmeImages(readmeBody, owner, name, branch, bannerUrl),
				versions,
				hasApk: versions === null ? null : hasDistributableApk(versions),
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

type RepoResource =
	| { state: 'fresh'; body: GithubRepository }
	| { state: 'unchanged'; branch: string }
	| { state: 'absent' };

async function readRepository(
	client: GithubClient,
	etags: EtagStore,
	target: RefreshTarget,
	resource: string
): Promise<RepoResource> {
	if (target.repo !== undefined) return { state: 'fresh', body: target.repo };

	const stored = await etags.read(resource);
	const etag = stored === null ? null : parseRepoEtag(stored).etag;

	try {
		const response = await client.getRepository(target.owner, target.name, etag);

		if (!response.isModified) {
			const branch = stored === null ? null : parseRepoEtag(stored).branch;
			if (branch === null) return { state: 'absent' };
			return { state: 'unchanged', branch };
		}

		await etags.write(resource, formatRepoEtag(response.etag, response.body.default_branch));
		return { state: 'fresh', body: response.body };
	} catch (cause) {
		if (cause instanceof GithubSkip) return { state: 'absent' };
		throw cause;
	}
}

const REPO_ETAG_SEPARATOR = ' ';

function formatRepoEtag(etag: string | null, branch: string): string | null {
	if (etag === null) return null;
	return `${branch}${REPO_ETAG_SEPARATOR}${etag}`;
}

function parseRepoEtag(stored: string): { etag: string; branch: string | null } {
	const index = stored.indexOf(REPO_ETAG_SEPARATOR);
	if (index === -1) return { etag: stored, branch: null };

	return { branch: stored.slice(0, index), etag: stored.slice(index + 1) };
}

async function iconFrom(
	client: GithubClient,
	tree: GithubTree,
	owner: string,
	name: string,
	branch: string
): Promise<string | null> {
	if (tree.truncated) return null;

	const read = async (path: string): Promise<string | null> => {
		const response = await readOptional(() => client.getRawFile(owner, name, path, branch));
		return response ?? null;
	};

	const readBlob = async (sha: string): Promise<string | null> => {
		const response = await readOptional(() => client.getBlob(owner, name, sha));
		return response ?? null;
	};

	const rasterUrl = await buildIconUrl(tree, owner, name, branch, readBlob);
	if (rasterUrl !== null) return rasterUrl;

	return buildVectorIcon(tree, read);
}

async function readResource<Body>(
	etags: EtagStore,
	resource: string,
	load: (
		etag: string | null
	) => Promise<{ isModified: true; body: Body; etag: string | null } | { isModified: false }>
): Promise<Resource<Body>> {
	try {
		const etag = await etags.read(resource);
		const response = await load(etag);

		if (!response.isModified) return { state: 'unchanged' };

		await etags.write(resource, response.etag);
		return { state: 'fresh', body: response.body };
	} catch (cause) {
		if (cause instanceof GithubSkip) return { state: 'absent' };
		throw cause;
	}
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
