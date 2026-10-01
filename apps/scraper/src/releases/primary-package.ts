import overrides from './primary-packages.json';

export type PackagedAsset = {
	versionId: string;
	packageName: string;
	signer: string | null;
	publishedAt: Date | null;
	isPrerelease: boolean;
};

export type Repository = {
	githubRepoId: number;
	owner: string;
	name: string;
};

type Candidate = {
	packageName: string;
	releaseCount: number;
	isNamedAfterRepository: boolean;
	isUsualSigner: boolean;
};

const MIN_NAME_LENGTH = 3;

const OVERRIDES = new Map(overrides.map((entry) => [entry.githubRepoId, entry.package]));

export function primaryPackage(assets: PackagedAsset[], repository: Repository): string | null {
	const override = OVERRIDES.get(repository.githubRepoId);
	if (override !== undefined) return override;

	const current = packagesOfNewestRelease(assets);
	const candidates = candidatesOf(assets, repository).filter((candidate) =>
		current.has(candidate.packageName)
	);
	const releaseBuilds = candidates.filter((candidate) => !isDebugPackage(candidate.packageName));
	const ranked = (releaseBuilds.length > 0 ? releaseBuilds : candidates).sort(byPrimacy);

	return ranked[0]?.packageName ?? null;
}

function packagesOfNewestRelease(assets: PackagedAsset[]): Set<string> {
	const newest = [...assets].sort(byRecency)[0];
	const builds = assets.filter((asset) => asset.versionId === newest?.versionId);

	return new Set(builds.map((build) => build.packageName));
}

function byRecency(left: PackagedAsset, right: PackagedAsset): number {
	if (left.isPrerelease !== right.isPrerelease) return left.isPrerelease ? 1 : -1;

	return (right.publishedAt?.getTime() ?? 0) - (left.publishedAt?.getTime() ?? 0);
}

function candidatesOf(assets: PackagedAsset[], repository: Repository): Candidate[] {
	const usualSigner = mostCommon(assets.map((asset) => asset.signer));
	const packages = groupBy(assets, (asset) => asset.packageName);

	return [...packages].map(([packageName, builds]) => ({
		packageName,
		releaseCount: new Set(builds.map((build) => build.versionId)).size,
		isNamedAfterRepository: isNamedAfter(packageName, repository),
		isUsualSigner: mostCommon(builds.map((build) => build.signer)) === usualSigner
	}));
}

function byPrimacy(left: Candidate, right: Candidate): number {
	const leftKey = primacyKey(left);
	const rightKey = primacyKey(right);

	for (const [index, value] of leftKey.entries()) {
		const other = rightKey[index]!;
		if (value !== other) return value < other ? -1 : 1;
	}

	return 0;
}

function primacyKey(candidate: Candidate): (number | string)[] {
	return [
		-candidate.releaseCount,
		Number(!candidate.isNamedAfterRepository),
		Number(!candidate.isUsualSigner),
		candidate.packageName.length,
		candidate.packageName
	];
}

function isNamedAfter(packageName: string, repository: Repository): boolean {
	const packageWords = simplified(packageName);
	return [repository.name, repository.owner]
		.map(simplified)
		.some((word) => word.length >= MIN_NAME_LENGTH && packageWords.includes(word));
}

function isDebugPackage(packageName: string): boolean {
	return packageName.toLowerCase().endsWith('.debug');
}

function simplified(value: string): string {
	return value.toLowerCase().replace(/[^a-z0-9]/g, '');
}

function mostCommon<Value>(values: Value[]): Value | undefined {
	const counts = groupBy(values, (value) => value);
	return [...counts].sort((left, right) => right[1].length - left[1].length)[0]?.[0];
}

function groupBy<Item, Key>(items: Item[], keyOf: (item: Item) => Key): Map<Key, Item[]> {
	const groups = new Map<Key, Item[]>();
	for (const item of items) {
		const key = keyOf(item);
		groups.set(key, [...(groups.get(key) ?? []), item]);
	}

	return groups;
}
