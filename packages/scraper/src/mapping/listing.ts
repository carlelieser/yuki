import { categorize } from '../detection/category.ts';
import type { GithubRepository } from '../github/types.ts';
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

export function parseTimestamp(value: string | null | undefined): Date | null {
	if (value === null || value === undefined) return null;

	const parsed = new Date(value);
	return Number.isNaN(parsed.getTime()) ? null : parsed;
}

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

export function buildTitle(name: string, readme: string | null): string {
	const heading = readme === null ? null : extractReadmeHeading(readme);
	if (heading !== null && heading.length <= MAX_TITLE_LENGTH) return heading;

	return humanizeRepoName(name);
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
