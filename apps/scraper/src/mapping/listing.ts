import { categorize } from '../detection/category.ts';
import { parseTimestamp, type GithubRepository } from '@yuki/github';
import type { ListingCategory, ListingConfidence } from '@yuki/db/schema';

export type MappedListing = {
	slug: string;
	githubRepoId: number;
	owner: string;
	name: string;
	title: string;
	author: string;
	authorUrl: string;
	description: string | null;
	repositoryUrl: string;
	homepageUrl: string | null;
	license: string | null;
	stars: number;
	confidence: ListingConfidence;
	category: ListingCategory | null;
	isFork: boolean;
	isArchived: boolean;
	repoPushedAt: Date | null;
};

const MAX_TITLE_LENGTH = 60;

export function slugify(value: string): string {
	return value
		.normalize('NFKD')
		.toLowerCase()
		.replace(/[^a-z0-9]+/g, '-')
		.replace(/^-+|-+$/g, '');
}

export function buildSlug(owner: string, name: string, githubRepoId: number): string {
	const slug = [slugify(owner), slugify(name)].filter((part) => part !== '').join('-');
	return slug === '' ? `repo-${githubRepoId}` : slug;
}

export function humanizeRepoName(name: string): string {
	const spaced = name
		.replace(/[_-]+/g, ' ')
		.replace(/([a-z0-9])([A-Z])/g, '$1 $2')
		.replace(/\s+/g, ' ')
		.trim();

	if (spaced === '') return name;

	return spaced
		.split(' ')
		.map((word) => (word === word.toUpperCase() ? word : word[0]?.toUpperCase() + word.slice(1)))
		.join(' ');
}

export function extractReadmeHeading(markdown: string): string | null {
	for (const line of markdown.split('\n')) {
		const match = line.match(/^#\s+(.+?)\s*$/);
		if (!match) continue;

		const heading = match[1]
			?.replace(/!\[[^\]]*\]\([^)]*\)/g, '')
			.replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
			.replace(/[*_`]/g, '')
			.trim();

		return heading === undefined || heading === '' ? null : heading;
	}

	return null;
}

const GENERIC_HEADINGS = new Set([
	'about',
	'overview',
	'introduction',
	'intro',
	'description',
	'features',
	'installation',
	'install',
	'usage',
	'getting started',
	'readme',
	'documentation',
	'docs',
	'license',
	'changelog',
	'download',
	'downloads',
	'screenshots',
	'requirements',
	'setup',
	'build',
	'credits',
	'contributing',
	'disclaimer',
	'notes',
	'todo',
	'简介',
	'介绍',
	'说明',
	'前言',
	'写在前面',
	'注意',
	'安装',
	'安装方法',
	'使用方法',
	'使用说明',
	'功能',
	'特性',
	'下载',
	'注意事项',
	'免责声明',
	'克隆仓库',
	'进入客户端目录'
]);

const VERSION_HEADING = /^v?\d+[\d.\-_]*(\s|$)/i;
const TAGLINE_SEPARATOR = /\s+[-–—·:|]\s+/;
const MAX_TITLE_WORDS = 5;

function stripEdgeEmoji(value: string): string {
	return value
		.replace(/^[\p{Extended_Pictographic}\p{Emoji_Presentation}️‍\s]+/u, '')
		.replace(/[\p{Extended_Pictographic}\p{Emoji_Presentation}️‍\s]+$/u, '')
		.trim();
}

function normalizeHeading(value: string): string {
	return value
		.toLowerCase()
		.replace(/[\s!?！？。.:：,，、_~-]+$/u, '')
		.trim();
}

function fold(value: string): string {
	return value
		.normalize('NFKD')
		.replace(/\p{Diacritic}/gu, '')
		.toLowerCase()
		.replace(/[^a-z0-9]/g, '');
}

function significantWords(value: string): Set<string> {
	const spaced = value.replace(/([a-z0-9])([A-Z])/g, '$1 $2').toLowerCase();
	return new Set(spaced.split(/[^a-z0-9]+/).filter((word) => word.length > 2));
}

function initialsOf(value: string): string {
	return value
		.split(/[^\p{L}\p{N}]+/u)
		.filter((word) => word !== '')
		.map((word) => word[0] ?? '')
		.join('')
		.toLowerCase();
}

function lettersOnly(value: string): string {
	return fold(value).replace(/[0-9]/g, '');
}

function isAcronymOf(short: string, long: string): boolean {
	const folded = fold(short);
	if (folded.length < 2) return false;

	return folded === initialsOf(long) || folded === lettersOnly(initialsOf(long));
}

function namesTheRepo(heading: string, name: string): boolean {
	const foldedHeading = fold(heading);
	const foldedName = fold(name);

	if (foldedHeading === foldedName) return true;
	if (foldedName !== '' && foldedHeading.includes(foldedName)) return true;
	if (foldedHeading !== '' && foldedName.includes(foldedHeading)) return true;

	const headingLetters = lettersOnly(heading);
	const nameLetters = lettersOnly(name);
	if (nameLetters !== '' && headingLetters.includes(nameLetters)) return true;
	if (headingLetters !== '' && nameLetters.includes(headingLetters)) return true;

	if (isAcronymOf(heading, name) || isAcronymOf(name, heading)) return true;

	const fromName = significantWords(name);
	if (fromName.size === 0) return true;

	const fromHeading = significantWords(heading);
	return [...fromName].some((word) => fromHeading.has(word));
}

export function titleFromHeading(heading: string, name: string): string | null {
	const cleaned = stripEdgeEmoji(heading);
	if (cleaned === '') return null;
	if (GENERIC_HEADINGS.has(normalizeHeading(cleaned))) return null;
	if (VERSION_HEADING.test(cleaned)) return null;
	if (cleaned.length > MAX_TITLE_LENGTH) return null;

	if (cleaned.split(/\s+/).length <= MAX_TITLE_WORDS) {
		return namesTheRepo(cleaned, name) ? cleaned : null;
	}

	const [lead] = cleaned.split(TAGLINE_SEPARATOR);
	const candidate = stripEdgeEmoji(lead ?? '');
	if (candidate === '' || candidate.split(/\s+/).length > MAX_TITLE_WORDS) return null;

	return namesTheRepo(candidate, name) ? candidate : null;
}

const REVERSE_DOMAIN_NAME = /^[a-z0-9]+(\.[a-z0-9_]+){2,}$/i;

export function resolveTitle(heading: string | null, name: string): string {
	const title = heading === null ? null : titleFromHeading(heading, name);
	if (title !== null) return title;

	const cleaned = heading === null ? '' : stripEdgeEmoji(heading);
	if (cleaned !== '' && REVERSE_DOMAIN_NAME.test(name) && cleaned.length <= MAX_TITLE_LENGTH) {
		return cleaned;
	}

	return humanizeRepoName(name);
}

export function buildTitle(name: string, readme: string | null): string {
	return resolveTitle(readme === null ? null : extractReadmeHeading(readme), name);
}

export function mapRepository(
	repo: GithubRepository,
	confidence: ListingConfidence,
	readme: string | null
): MappedListing {
	const homepage = repo.homepage?.trim();

	return {
		slug: buildSlug(repo.owner.login, repo.name, repo.id),
		githubRepoId: repo.id,
		owner: repo.owner.login,
		name: repo.name,
		title: buildTitle(repo.name, readme),
		author: repo.owner.login,
		authorUrl: repo.owner.html_url,
		description: repo.description,
		repositoryUrl: repo.html_url,
		homepageUrl: homepage === undefined || homepage === '' ? null : homepage,
		license: repo.license?.spdx_id ?? null,
		stars: repo.stargazers_count,
		confidence,
		category: categorize({
			description: repo.description,
			topics: repo.topics ?? [],
			readme
		}),
		isFork: repo.fork,
		isArchived: repo.archived,
		repoPushedAt: parseTimestamp(repo.pushed_at)
	};
}
