import { describe, expect, it } from 'vitest';
import { GITHUB_EMAIL_UNVERIFIED, rejectUnverifiedGithubSignUp } from './github-policy.ts';

function github(action: string) {
	return { action, method: 'oauth', oauth: { providerId: 'github' } };
}

describe('rejectUnverifiedGithubSignUp', () => {
	it('rejects a new account from a GitHub email that GitHub has not verified', () => {
		const result = rejectUnverifiedGithubSignUp({
			user: { emailVerified: false },
			source: github('create-user')
		});

		expect(result?.error).toBe(GITHUB_EMAIL_UNVERIFIED);
	});

	it('allows a new account from a verified GitHub email', () => {
		const result = rejectUnverifiedGithubSignUp({
			user: { emailVerified: true },
			source: github('create-user')
		});

		expect(result).toBeUndefined();
	});

	it('leaves linking and returning sign-ins to the account linking rules', () => {
		for (const action of ['link-account', 'sign-in']) {
			const result = rejectUnverifiedGithubSignUp({
				user: { emailVerified: false },
				source: github(action)
			});

			expect(result).toBeUndefined();
		}
	});

	it('does not apply to email sign-ups, which verify by email', () => {
		const result = rejectUnverifiedGithubSignUp({
			user: { emailVerified: false },
			source: { action: 'create-user', method: 'email-password' }
		});

		expect(result).toBeUndefined();
	});
});
