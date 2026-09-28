type UserInfo = {
	user: { emailVerified?: boolean };
	source: { action: string; method: string; oauth?: { providerId: string } };
};

export const GITHUB_EMAIL_UNVERIFIED = 'github_email_unverified';

export function rejectUnverifiedGithubSignUp({ user, source }: UserInfo) {
	const isGithubSignUp =
		source.method === 'oauth' &&
		source.oauth?.providerId === 'github' &&
		source.action === 'create-user';

	if (!isGithubSignUp || user.emailVerified) return;

	return {
		error: GITHUB_EMAIL_UNVERIFIED,
		errorDescription: 'Verify your email address on GitHub before signing up with it.'
	};
}
