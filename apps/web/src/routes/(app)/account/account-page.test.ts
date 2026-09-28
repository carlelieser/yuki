import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isActionFailure, isRedirect } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { SessionUser } from '@yuki/auth';

const { getAuth, api } = vi.hoisted(() => {
	const api = { listUserAccounts: vi.fn(), linkSocialAccount: vi.fn(), unlinkAccount: vi.fn() };
	return { getAuth: vi.fn(() => ({ api })), api };
});

vi.mock('$lib/server/auth.ts', () => ({ getAuth }));

const { load, actions } = await import('./+page.server.ts');

type Event = Parameters<typeof load>[0];

const user = { id: 'user-1', email: 'ada@yuki.test' } as SessionUser;
const credential = { id: 'acc-1', providerId: 'credential' };
const github = {
	id: 'acc-2',
	providerId: 'github',
	providerUsername: 'ada',
	providerProfileUrl: 'https://github.com/ada'
};

function event(path: string, init?: { body?: Record<string, string>; signedIn?: boolean }) {
	const url = new URL(`http://yuki.test${path}`);
	const request = new Request(url, {
		method: init?.body ? 'POST' : 'GET',
		body: init?.body ? new URLSearchParams(init.body) : undefined
	});
	return { url, request, locals: { user: init?.signedIn === false ? null : user } } as Event;
}

async function thrown(run: () => unknown) {
	try {
		await run();
	} catch (value) {
		return value;
	}
	return expect.unreachable('expected a redirect');
}

beforeEach(() => {
	vi.clearAllMocks();
	process.env.GITHUB_CLIENT_ID = 'client-id';
	process.env.GITHUB_CLIENT_SECRET = 'client-secret';
});

describe('account page', () => {
	it('sends signed-out visitors to sign in', async () => {
		const result = await thrown(() => load(event('/account', { signedIn: false })));

		expect(isRedirect(result) && result.location).toBe('/signin?redirectTo=%2Faccount');
	});

	it('shows the connected GitHub username', async () => {
		api.listUserAccounts.mockResolvedValue([credential, github]);

		const data = await load(event('/account'));

		expect(data).toMatchObject({
			github: { id: 'acc-2', username: 'ada', profileUrl: 'https://github.com/ada' },
			canUnlink: true,
			isGithubEnabled: true
		});
	});

	it('does not offer to disconnect the only way to sign in', async () => {
		api.listUserAccounts.mockResolvedValue([github]);

		expect((await load(event('/account'))).canUnlink).toBe(false);
	});

	it('shows no GitHub account for email-only users', async () => {
		api.listUserAccounts.mockResolvedValue([credential]);

		expect((await load(event('/account'))).github).toBeNull();
	});

	it('explains why GitHub could not be connected', async () => {
		api.listUserAccounts.mockResolvedValue([credential]);

		const data = await load(event('/account?error=account_already_linked_to_different_user'));

		expect(data.githubError).toContain('already connected to another Yuki account');
	});
});

describe('connecting GitHub', () => {
	it('sends the user to GitHub and back to the account page', async () => {
		api.linkSocialAccount.mockResolvedValue({ url: 'https://github.com/login/oauth/authorize' });

		const result = await thrown(() => actions.link(event('/account?/link', { body: {} })));

		expect(isRedirect(result) && result.location).toBe('https://github.com/login/oauth/authorize');
		expect(api.linkSocialAccount).toHaveBeenCalledWith(
			expect.objectContaining({
				body: expect.objectContaining({
					provider: 'github',
					callbackURL: '/account?linked=github',
					errorCallbackURL: '/account'
				})
			})
		);
	});
});

describe('disconnecting GitHub', () => {
	function unlink() {
		return actions.unlink(event('/account?/unlink', { body: { accountId: 'acc-2' } }));
	}

	it('removes the linked account', async () => {
		api.unlinkAccount.mockResolvedValue({ status: true });

		expect(await unlink()).toEqual({ isUnlinked: true });
		expect(api.unlinkAccount).toHaveBeenCalledWith(
			expect.objectContaining({ body: { accountId: 'acc-2' } })
		);
	});

	it.each([
		['FAILED_TO_UNLINK_LAST_ACCOUNT', 'Set a password'],
		['SESSION_NOT_FRESH', 'sign in again']
	])('explains %s', async (code, text) => {
		api.unlinkAccount.mockRejectedValue(new APIError('BAD_REQUEST', { code, message: code }));

		const result: unknown = await unlink();

		expect(isActionFailure(result)).toBe(true);
		expect(result).toMatchObject({ data: { unlinkError: expect.stringContaining(text) } });
	});
});
