import type { EvidenceKind } from '@yuki/db/schema';

export const RESULTS_PER_PAGE = 100;
export const MAX_PAGES = 10;
export const RESULT_CAP = RESULTS_PER_PAGE * MAX_PAGES;

export type CodeSearchQuery = {
	q: string;
	evidence: EvidenceKind;
	detail: string;
};

export type RepoSearchQuery = {
	q: string;
	evidence: EvidenceKind;
	detail: string;
};

export function buildCodeSearchQueries(): CodeSearchQuery[] {
	return [
		{
			q: 'rikka.shizuku.ShizukuProvider filename:AndroidManifest.xml',
			evidence: 'provider_class',
			detail: 'rikka.shizuku.ShizukuProvider in AndroidManifest.xml'
		},
		{
			q: 'dev.rikka.shizuku filename:build.gradle',
			evidence: 'gradle_dependency',
			detail: 'dev.rikka.shizuku in build.gradle'
		},
		{
			q: 'dev.rikka.shizuku filename:build.gradle.kts',
			evidence: 'gradle_dependency',
			detail: 'dev.rikka.shizuku in build.gradle.kts'
		},
		{
			q: 'dev.rikka.shizuku filename:libs.versions.toml',
			evidence: 'gradle_dependency',
			detail: 'dev.rikka.shizuku in libs.versions.toml'
		},
		{
			q: 'moe.shizuku.api filename:build.gradle',
			evidence: 'legacy_gradle_dependency',
			detail: 'moe.shizuku.api in build.gradle'
		},
		{
			q: 'moe.shizuku.api filename:build.gradle.kts',
			evidence: 'legacy_gradle_dependency',
			detail: 'moe.shizuku.api in build.gradle.kts'
		}
	];
}

export function buildRepoSearchQueries(): RepoSearchQuery[] {
	return [
		{ q: 'topic:shizuku', evidence: 'repository_topic', detail: 'topic:shizuku' },
		{
			q: 'shizuku in:name,description,readme',
			evidence: 'readme_mention',
			detail: 'shizuku in name, description, or readme'
		}
	];
}

const ANDROID_BUILD_FILENAMES = ['build.gradle', 'build.gradle.kts', 'libs.versions.toml'];

export function isAndroidStructurePath(path: string): boolean {
	const filename = path.split('/').pop() ?? '';
	if (filename === 'AndroidManifest.xml') return true;
	return ANDROID_BUILD_FILENAMES.includes(filename);
}

const MARKDOWN_EXTENSIONS = ['.md', '.markdown', '.mdx', '.rst', '.txt'];

export function isMarkdownPath(path: string): boolean {
	const lowered = path.toLowerCase();
	return MARKDOWN_EXTENSIONS.some((extension) => lowered.endsWith(extension));
}

export function isSourceFilenameMatch(path: string): boolean {
	const filename = path.split('/').pop()?.toLowerCase() ?? '';
	if (!filename.endsWith('.kt') && !filename.endsWith('.java')) return false;
	return filename.includes('shizuku');
}

export function isSliceTruncated(totalCount: number): boolean {
	return totalCount > RESULT_CAP;
}

const DAY_MS = 24 * 60 * 60 * 1000;

export const GITHUB_EPOCH = new Date('2008-01-01T00:00:00.000Z');

export type DateRange = { since: Date; until: Date };

function toDay(value: Date): string {
	return value.toISOString().slice(0, 10);
}

export function withCreatedRange(q: string, range: DateRange): string {
	return `${q} created:${toDay(range.since)}..${toDay(range.until)}`;
}

export type SizeRange = { from: number; to: number | null };

export const FULL_SIZE_RANGE: SizeRange = { from: 0, to: null };

export function withSizeRange(q: string, range: SizeRange): string {
	if (range.to === null) return `${q} size:>=${range.from}`;
	return `${q} size:${range.from}..${range.to}`;
}

export const MAX_TRACKED_FILE_SIZE = 1_048_576;

export function isSizeIndivisible(range: SizeRange): boolean {
	if (range.to === null) return range.from >= MAX_TRACKED_FILE_SIZE;
	return range.to - range.from <= 1;
}

export function splitSizeRange(range: SizeRange): [SizeRange, SizeRange] {
	if (range.to === null) {
		const boundary = range.from === 0 ? 1024 : range.from * 4;
		return [
			{ from: range.from, to: boundary },
			{ from: boundary + 1, to: null }
		];
	}

	const midpoint = range.from + Math.floor((range.to - range.from) / 2);
	return [
		{ from: range.from, to: midpoint },
		{ from: midpoint + 1, to: range.to }
	];
}

export function describeSizeRange(range: SizeRange): string {
	return range.to === null ? `${range.from}+` : `${range.from}..${range.to}`;
}

export function isIndivisible(range: DateRange): boolean {
	return range.until.getTime() - range.since.getTime() <= DAY_MS;
}

export function splitRange(range: DateRange): [DateRange, DateRange] {
	const midpoint = new Date(
		range.since.getTime() + Math.floor((range.until.getTime() - range.since.getTime()) / 2)
	);

	return [
		{ since: range.since, until: midpoint },
		{ since: new Date(midpoint.getTime() + DAY_MS), until: range.until }
	];
}
