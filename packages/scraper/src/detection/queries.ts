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

const SIZE_SLICES = ['<50', '50..200', '200..1000', '>1000'];

function sliceBySize(base: string, evidence: EvidenceKind, detail: string): CodeSearchQuery[] {
	return SIZE_SLICES.map((size) => ({
		q: `${base} size:${size}`,
		evidence,
		detail: `${detail} (size:${size})`
	}));
}

export function buildCodeSearchQueries(): CodeSearchQuery[] {
	return [
		...sliceBySize(
			'rikka.shizuku.ShizukuProvider filename:AndroidManifest.xml',
			'provider_class',
			'rikka.shizuku.ShizukuProvider in AndroidManifest.xml'
		),
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
	return [{ q: 'topic:shizuku', evidence: 'repository_topic', detail: 'topic:shizuku' }];
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
