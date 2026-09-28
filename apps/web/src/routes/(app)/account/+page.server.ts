import { fail, redirect } from '@sveltejs/kit';
import { getGithubCredentials } from '@yuki/auth';
import { APIError } from 'better-auth/api';
import type { Actions, PageServerLoad } from './$types';
import { getAuth } from '$lib/server/auth.ts';
import { requireUser } from '$lib/server/auth-guard.ts';
import { githubErrorMessage } from '$lib/github-errors.ts';

type LinkedAccount = {
	id: string;
	providerId: string;
	providerUsername?: string | null;
	providerProfileUrl?: string | null;
};

const unlinkMessages: Record<string, string> = {
	FAILED_TO_UNLINK_LAST_ACCOUNT:
		'GitHub is your only way to sign in. Set a password with “Forgot password” first.',
	SESSION_NOT_FRESH: 'For your security, sign in again before disconnecting GitHub.'
};

function githubAccount(accounts: LinkedAccount[]) {
	const github = accounts.find((account) => account.providerId === 'github');
	if (!github) return null;

	return {
		id: github.id,
		username: github.providerUsername ?? null,
		profileUrl: github.providerProfileUrl ?? null
	};
}

export const load = (async (event) => {
	requireUser(event);

	const accounts: LinkedAccount[] = await getAuth().api.listUserAccounts({
		headers: event.request.headers
	});

	return {
		github: githubAccount(accounts),
		canUnlink: accounts.length > 1,
		isGithubEnabled: getGithubCredentials() !== undefined,
		isLinked: event.url.searchParams.get('linked') === 'github',
		githubError: githubErrorMessage(event.url.searchParams.get('error'))
	};
}) satisfies PageServerLoad;

export const actions = {
	link: async (event) => {
		requireUser(event);

		const { url } = await getAuth().api.linkSocialAccount({
			body: {
				provider: 'github',
				callbackURL: '/account?linked=github',
				errorCallbackURL: '/account',
				disableRedirect: true
			},
			headers: event.request.headers
		});

		redirect(303, url);
	},

	unlink: async (event) => {
		requireUser(event);

		const form = await event.request.formData();
		const accountId = form.get('accountId');
		if (typeof accountId !== 'string') return fail(400, { unlinkError: 'Choose an account.' });

		try {
			await getAuth().api.unlinkAccount({ body: { accountId }, headers: event.request.headers });
		} catch (cause) {
			if (!(cause instanceof APIError)) throw cause;

			const code = String(cause.body?.code ?? '');
			return fail(400, { unlinkError: unlinkMessages[code] ?? 'Could not disconnect GitHub.' });
		}

		return { isUnlinked: true };
	}
} satisfies Actions;
