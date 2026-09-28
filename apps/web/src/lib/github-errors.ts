const messages: Record<string, string> = {
	access_denied: 'GitHub sign-in was cancelled.',
	account_not_linked:
		'An account with this email already exists. Sign in with your password, then connect GitHub from your account page.',
	github_email_unverified: 'Verify your email address on GitHub, then try again.',
	unable_to_link_account: 'Verify your email address on GitHub, then try again.',
	account_already_linked_to_different_user:
		'This GitHub account is already connected to another Yuki account.',
	email_not_found: 'GitHub did not share an email address. Add one to your GitHub account.'
};

const fallback = 'Something went wrong with GitHub. Try again.';

export function githubErrorMessage(code: string | null): string | null {
	if (!code) return null;
	return messages[code] ?? fallback;
}
