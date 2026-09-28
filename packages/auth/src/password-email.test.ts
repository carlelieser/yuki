import { describe, expect, it } from 'vitest';
import { passwordEmail } from './password-email.ts';

const URL = 'https://yukistore.org/api/auth/reset-password/token';

describe('passwordEmail', () => {
	it('asks an account with a password to reset it', () => {
		const email = passwordEmail(true, URL);

		expect(email.subject).toBe('Reset your Yuki password');
		expect(email.content.action).toEqual({ label: 'Reset password', url: URL });
	});

	it('asks an account without a password to set one', () => {
		const email = passwordEmail(false, URL);

		expect(email.subject).toBe('Set a password for Yuki');
		expect(email.content.action).toEqual({ label: 'Set password', url: URL });
	});
});
