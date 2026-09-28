export type GithubIdentity = { username: string; profileUrl: string };

export type FetchGithubIdentity = (accessToken: string) => Promise<GithubIdentity | null>;

type AccountWrite = { providerId?: string; accessToken?: string | null };

const GITHUB_USER_URL = 'https://api.github.com/user';
const GITHUB_TIMEOUT_MILLIS = 5000;

export async function fetchGithubIdentity(accessToken: string): Promise<GithubIdentity | null> {
	let response: Response;
	try {
		response = await fetch(GITHUB_USER_URL, {
			headers: {
				Accept: 'application/vnd.github+json',
				Authorization: `Bearer ${accessToken}`,
				'User-Agent': 'yuki'
			},
			signal: AbortSignal.timeout(GITHUB_TIMEOUT_MILLIS)
		});
	} catch {
		return null;
	}

	if (!response.ok) return null;

	const profile = (await response.json()) as { login?: unknown; html_url?: unknown };
	if (typeof profile.login !== 'string' || typeof profile.html_url !== 'string') return null;

	return { username: profile.login, profileUrl: profile.html_url };
}

export function githubAccountHooks(fetchIdentity: FetchGithubIdentity = fetchGithubIdentity) {
	async function before(account: AccountWrite) {
		if (account.providerId !== 'github' || !account.accessToken) return;

		const identity = await fetchIdentity(account.accessToken);
		const profile = identity
			? { providerUsername: identity.username, providerProfileUrl: identity.profileUrl }
			: {};

		return { data: { ...profile, accessToken: null, refreshToken: null } };
	}

	return { create: { before }, update: { before } };
}
