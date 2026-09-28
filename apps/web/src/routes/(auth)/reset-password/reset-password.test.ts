import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isRedirect } from '@sveltejs/kit';

const { getAuth, resetPassword } = vi.hoisted(() => {
	const resetPassword = vi.fn();
	return { getAuth: vi.fn(() => ({ api: { resetPassword } })), resetPassword };
});

vi.mock('$lib/server/auth.ts', () => ({ getAuth }));

const { load, actions } = await import('./+page.server.ts');

type LoadEvent = Parameters<typeof load>[0];
type ActionEvent = Parameters<typeof actions.default>[0];

function submit(query: string) {
	const url = new URL(`https://yuki.test/reset-password${query}`);
	const body = new URLSearchParams({
		token: 'token-1',
		password: 'a-long-password-1',
		confirmPassword: 'a-long-password-1'
	});
	return { url, request: new Request(url, { method: 'POST', body }) } as unknown as ActionEvent;
}

async function redirectOf(event: ActionEvent): Promise<string> {
	try {
		await actions.default(event);
	} catch (thrown) {
		if (isRedirect(thrown)) return thrown.location;
		throw thrown;
	}
	return expect.unreachable('expected a redirect');
}

beforeEach(() => {
	resetPassword.mockReset().mockResolvedValue({ status: true });
});

describe('reset password page', () => {
	it('says "set" when an account is adding its first password', async () => {
		const url = new URL('https://yuki.test/reset-password?mode=set&token=token-1');

		const data = await load({ url } as LoadEvent);

		expect(data.isSettingPassword).toBe(true);
	});

	it('returns to the account page after setting a first password', async () => {
		expect(await redirectOf(submit('?mode=set&token=token-1'))).toBe('/account?password=set');
	});

	it('returns to sign-in after a reset', async () => {
		expect(await redirectOf(submit('?token=token-1'))).toBe('/signin');
	});
});
