export function requireGithubToken(): string {
	const githubToken = process.env.GITHUB_TOKEN;
	if (!githubToken) {
		throw new Error('Missing required environment variable GITHUB_TOKEN');
	}
	return githubToken;
}
