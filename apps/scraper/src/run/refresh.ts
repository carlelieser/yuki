import {
	hasAndroidStructure,
	mergeEvidence,
	scoreConfidence,
	type DetectedEvidence
} from '../detection/evidence.ts';
import { GithubSkip, type GithubClient } from '@yuki/github';
import { iconFrom, readOptional } from './icon-source.ts';
import { mapRepository } from '../mapping/listing.ts';
import { extractReadmeImages, findBannerUrl } from '../mapping/readme-images.ts';
import {
	buildBlobUrl,
	buildIconUrl,
	collectLfsPaths,
	findDeclaredIconPaths,
	findManifestPath,
	findRasterForReference,
	pickBestDeclared,
	readAdaptiveRasterLayers,
	readManifestIcon,
	readRasterReferences
} from '../mapping/icon.ts';
import { composeAdaptiveRaster, toPngDataUri } from '../mapping/adaptive-raster.ts';
import { buildVectorIcon } from '../mapping/adaptive-vector.ts';
import { hasDistributableApk, mapReleases } from '@yuki/github';
import type { GithubRepository, GithubTree, MappedVersion } from '@yuki/github';
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
	githubRepoId?: number;
	evidence?: DetectedEvidence[];
	repo?: GithubRepository;
	packageName?: string | null;
};

export type ApkPackageReader = (downloadUrl: string) => Promise<string | null>;

type Resource<Body> = { state: 'fresh'; body: Body } | { state: 'unchanged' } | { state: 'absent' };

export async function refreshListing(
	client: GithubClient,
	etags: EtagStore,
	target: RefreshTarget,
	readApkPackage?: ApkPackageReader
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
		const lfsPaths =
			tree.state === 'fresh' && !tree.body.truncated
				? await collectLfsPaths(tree.body, (sha) =>
						readOptional(() => client.getBlob(owner, name, sha)).then(
							(response) => response ?? null
						)
					)
				: new Set<string>();
		const bannerUrl =
			readmeBody === null ? null : findBannerUrl(readmeBody, owner, name, branch, lfsPaths);
		const isAndroidApp = androidVerdict(tree);

		return {
			kind: 'updated',
			input: {
				owner,
				name,
				githubRepoId: target.githubRepoId,
				listing:
					repo.state === 'fresh'
						? mapRepository(repo.body, scoreConfidence(evidence), readmeBody)
						: null,
				iconUrl:
					tree.state === 'fresh' ? await iconFrom(client, tree.body, owner, name, branch) : null,
				bannerUrl: readme.state === 'unchanged' ? null : bannerUrl,
				packageName: await packageNameFrom(target, versions, readApkPackage),
				screenshots:
					readme.state === 'unchanged'
						? null
						: readmeBody === null
							? []
							: extractReadmeImages(readmeBody, owner, name, branch, bannerUrl, lfsPaths),
				versions,
				hasApk: versions === null ? null : hasDistributableApk(versions),
				isAndroidApp,
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

async function packageNameFrom(
	target: RefreshTarget,
	versions: MappedVersion[] | null,
	read: ApkPackageReader | undefined
): Promise<string | null> {
	if (read === undefined) return null;
	if (target.packageName != null) return null;
	if (versions === null) return null;

	const downloadUrl = newestDownloadUrl(versions);
	if (downloadUrl === null) return null;

	try {
		return await read(downloadUrl);
	} catch {
		return null;
	}
}

function newestDownloadUrl(versions: MappedVersion[]): string | null {
	const published = versions.filter(
		(version) => version.downloadUrl !== null && !version.isPrerelease
	);
	const pool = published.length > 0 ? published : versions;

	for (const version of pool) {
		if (version.downloadUrl !== null) return version.downloadUrl;
	}

	return null;
}

function androidVerdict(tree: Resource<GithubTree>): boolean | null {
	if (tree.state !== 'fresh') return null;

	const paths = tree.body.tree.map((entry) => entry.path);
	if (hasAndroidStructure(paths)) return true;

	return tree.body.truncated ? null : false;
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
