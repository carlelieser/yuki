import { afterEach, describe, expect, it, vi } from 'vitest';
import { fetchGithubIdentity, githubAccountHooks, type GithubIdentity } from './github-account.ts';

const identity: GithubIdentity = { username: 'octocat', profileUrl: 'https://github.com/octocat' };

afterEach(() => {
	vi.unstubAllGlobals();
});

describe('fetchGithubIdentity', () => {
	it('reads the login and profile url from the GitHub user endpoint', async () => {
		const fetch = vi.fn(async () =>
			Response.json({ login: 'octocat', html_url: 'https://github.com/octocat' })
		);
		vi.stubGlobal('fetch', fetch);

		expect(await fetchGithubIdentity('token')).toEqual(identity);
		expect(fetch).toHaveBeenCalledWith(
			'https://api.github.com/user',
			expect.objectContaining({
				headers: expect.objectContaining({ Authorization: 'Bearer token' })
			})
		);
	});

	it('returns null when GitHub rejects the token', async () => {
		vi.stubGlobal('fetch', async () => new Response(null, { status: 401 }));

		expect(await fetchGithubIdentity('token')).toBeNull();
	});

	it('returns null when GitHub is unreachable', async () => {
		vi.stubGlobal('fetch', async () => {
			throw new TypeError('fetch failed');
		});

		expect(await fetchGithubIdentity('token')).toBeNull();
	});
});

describe('githubAccountHooks', () => {
	const hooks = githubAccountHooks(async () => identity);

	it('stores the GitHub profile and drops the tokens when an account is created', async () => {
		const result = await hooks.create.before({
			providerId: 'github',
			accessToken: 'gho_token'
		});

		expect(result).toEqual({
			data: {
				providerUsername: 'octocat',
				providerProfileUrl: 'https://github.com/octocat',
				accessToken: null,
				refreshToken: null
			}
		});
	});

	it('refreshes the profile when a returning GitHub sign-in updates the account', async () => {
		const result = await hooks.update.before({ providerId: 'github', accessToken: 'gho_token' });

		expect(result?.data.providerUsername).toBe('octocat');
	});

	it('keeps the stored profile when GitHub cannot be reached', async () => {
		const offline = githubAccountHooks(async () => null);

		const result = await offline.update.before({ providerId: 'github', accessToken: 'gho_token' });

		expect(result).toEqual({ data: { accessToken: null, refreshToken: null } });
	});

	it('leaves other providers untouched', async () => {
		expect(
			await hooks.create.before({ providerId: 'google', accessToken: 'ya29' })
		).toBeUndefined();
		expect(await hooks.create.before({ providerId: 'credential' })).toBeUndefined();
	});

	it('ignores GitHub account updates that carry no token', async () => {
		expect(await hooks.update.before({ providerId: 'github' })).toBeUndefined();
	});
});
