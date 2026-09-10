import { describe, expect, it } from 'vitest';
import {
	buildSlug,
	buildTitle,
	extractReadmeHeading,
	humanizeRepoName,
	mapRepository
} from './listing.ts';
import type { GithubRepository } from '@yuki/github';

function repository(overrides: Partial<GithubRepository> = {}): GithubRepository {
	return {
		id: 42,
		name: 'shizuku-manager',
		full_name: 'acme/shizuku-manager',
		description: 'Manage things',
		html_url: 'https://github.com/acme/shizuku-manager',
		homepage: null,
		default_branch: 'main',
		stargazers_count: 120,
		fork: false,
		archived: false,
		pushed_at: '2026-02-01T00:00:00Z',
		license: { spdx_id: 'MIT' },
		owner: { login: 'acme', html_url: 'https://github.com/acme' },
		...overrides
	};
}

describe('buildSlug', () => {
	it('joins owner and name', () => {
		expect(buildSlug('Acme', 'Shizuku_Manager', 42)).toBe('acme-shizuku-manager');
	});

	it('falls back to the repo id when the name has no slug-safe characters', () => {
		expect(buildSlug('日本', '語', 42)).toBe('repo-42');
	});
});

describe('humanizeRepoName', () => {
	it('splits separators and camel case', () => {
		expect(humanizeRepoName('shizuku-manager')).toBe('Shizuku Manager');
		expect(humanizeRepoName('LogFox')).toBe('Log Fox');
	});

	it('leaves acronyms uppercase', () => {
		expect(humanizeRepoName('ADB-tool')).toBe('ADB Tool');
	});
});

describe('extractReadmeHeading', () => {
	it('reads the first h1 and strips markdown decoration', () => {
		expect(extractReadmeHeading('# **Aurora** Store\n\ntext')).toBe('Aurora Store');
	});

	it('drops badge images from the heading', () => {
		expect(extractReadmeHeading('# Nrfr ![badge](https://img.shields.io/x)')).toBe('Nrfr');
	});

	it('returns null when there is no h1', () => {
		expect(extractReadmeHeading('## Sub\n\ntext')).toBeNull();
	});
});

describe('buildTitle', () => {
	it('prefers a short readme heading', () => {
		expect(buildTitle('aurora-store', '# Aurora Store')).toBe('Aurora Store');
	});

	it('ignores a long heading and humanizes the repo name instead', () => {
		const longHeading = `# ${'x'.repeat(80)}`;

		expect(buildTitle('aurora-store', longHeading)).toBe('Aurora Store');
	});

	it('humanizes the repo name when there is no readme', () => {
		expect(buildTitle('aurora-store', null)).toBe('Aurora Store');
	});
});

describe('mapRepository', () => {
	it('maps the fields the storefront needs', () => {
		const mapped = mapRepository(repository(), 'strong', null);

		expect(mapped).toMatchObject({
			slug: 'acme-shizuku-manager',
			githubRepoId: 42,
			owner: 'acme',
			name: 'shizuku-manager',
			title: 'Shizuku Manager',
			author: 'acme',
			authorUrl: 'https://github.com/acme',
			description: 'Manage things',
			repositoryUrl: 'https://github.com/acme/shizuku-manager',
			license: 'MIT',
			stars: 120,
			confidence: 'strong',
			isFork: false,
			isArchived: false
		});
		expect(mapped.repoPushedAt).toEqual(new Date('2026-02-01T00:00:00Z'));
	});

	it('normalises a blank homepage to null', () => {
		expect(mapRepository(repository({ homepage: '   ' }), 'strong', null).homepageUrl).toBeNull();
	});

	it('keeps a real homepage', () => {
		const mapped = mapRepository(repository({ homepage: 'https://acme.dev' }), 'strong', null);

		expect(mapped.homepageUrl).toBe('https://acme.dev');
	});

	it('survives a repository that omits pushed_at, as code search embeds it', () => {
		const partial = repository();
		delete (partial as { pushed_at?: unknown }).pushed_at;

		expect(mapRepository(partial, 'strong', null).repoPushedAt).toBeNull();
	});

	it('handles a missing license', () => {
		expect(mapRepository(repository({ license: null }), 'weak', null).license).toBeNull();
	});
});
