export type ParsedVersion = {
	release: number[];
	prerelease: string[] | null;
};

const VERSION_PATTERN = /^[^0-9]*?(\d+(?:\.\d+)*)(?:[-_.]?([0-9a-z.+-]+))?$/i;

export function parseVersion(tag: string): ParsedVersion | null {
	const match = VERSION_PATTERN.exec(tag.trim());
	if (match === null) return null;

	const digits = match[1];
	if (digits === undefined) return null;

	const release = digits.split('.').map(Number);
	if (release.some((part) => !Number.isSafeInteger(part))) return null;

	const suffix = match[2];
	if (suffix === undefined || suffix === '') return { release, prerelease: null };

	const build = suffix.split('+')[0];
	if (build === undefined || build === '') return { release, prerelease: null };

	return { release, prerelease: build.toLowerCase().split('.') };
}

function compareRelease(left: number[], right: number[]): number {
	const length = Math.max(left.length, right.length);

	for (let index = 0; index < length; index += 1) {
		const difference = (left[index] ?? 0) - (right[index] ?? 0);
		if (difference !== 0) return difference < 0 ? -1 : 1;
	}

	return 0;
}

function comparePrereleaseIdentifier(left: string, right: string): number {
	const leftNumeric = /^\d+$/.test(left);
	const rightNumeric = /^\d+$/.test(right);

	if (leftNumeric && rightNumeric) {
		const difference = Number(left) - Number(right);
		return difference === 0 ? 0 : difference < 0 ? -1 : 1;
	}

	if (leftNumeric) return -1;
	if (rightNumeric) return 1;

	return left === right ? 0 : left < right ? -1 : 1;
}

function comparePrerelease(left: string[] | null, right: string[] | null): number {
	if (left === null && right === null) return 0;
	if (left === null) return 1;
	if (right === null) return -1;

	const length = Math.max(left.length, right.length);

	for (let index = 0; index < length; index += 1) {
		const leftPart = left[index];
		const rightPart = right[index];

		if (leftPart === undefined) return -1;
		if (rightPart === undefined) return 1;

		const difference = comparePrereleaseIdentifier(leftPart, rightPart);
		if (difference !== 0) return difference;
	}

	return 0;
}

export function compareVersions(left: ParsedVersion, right: ParsedVersion): number {
	const release = compareRelease(left.release, right.release);
	return release === 0 ? comparePrerelease(left.prerelease, right.prerelease) : release;
}

export function isNewerTag(candidate: string, installed: string): boolean {
	if (candidate === installed) return false;

	const parsedCandidate = parseVersion(candidate);
	const parsedInstalled = parseVersion(installed);

	if (parsedCandidate === null || parsedInstalled === null) return false;

	return compareVersions(parsedCandidate, parsedInstalled) > 0;
}
