import { describe, expect, it } from 'vitest';
import { githubErrorMessage } from './github-errors.ts';

describe('githubErrorMessage', () => {
	it('shows nothing when there is no error', () => {
		expect(githubErrorMessage(null)).toBeNull();
	});

	it('tells an existing user how to connect GitHub instead', () => {
		expect(githubErrorMessage('account_not_linked')).toContain('Sign in with your password');
	});

	it('asks the user to verify their GitHub email', () => {
		expect(githubErrorMessage('github_email_unverified')).toContain('Verify your email');
	});

	it('falls back to a generic message for unknown codes', () => {
		expect(githubErrorMessage('state_mismatch')).toBe(
			'Something went wrong with GitHub. Try again.'
		);
	});
});
