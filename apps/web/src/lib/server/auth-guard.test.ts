import { describe, expect, it } from 'vitest';
import { isHttpError, isRedirect, type RequestEvent } from '@sveltejs/kit';
import { requireDeveloper, requireUser } from './auth-guard.ts';
import type { SessionUser } from '@yuki/auth';

function eventFor(user: SessionUser | null, url = 'http://localhost/reviews/new?draft=1') {
	return {
		locals: { user },
		url: new URL(url)
	} as unknown as RequestEvent;
}

const member = { id: 'u1', name: 'Ada', email: 'ada@example.com', role: 'user' } as SessionUser;
const developer = { ...member, id: 'u2', role: 'developer' } as SessionUser;

describe('requireUser', () => {
	it('returns the user when one is signed in', () => {
		expect(requireUser(eventFor(member))).toBe(member);
	});

	it('redirects anonymous visitors back to the page they wanted', () => {
		try {
			requireUser(eventFor(null));
			expect.unreachable('requireUser should have redirected');
		} catch (thrown) {
			expect(isRedirect(thrown)).toBe(true);
			if (isRedirect(thrown)) {
				expect(thrown.status).toBe(303);
				expect(thrown.location).toBe('/signin?redirectTo=%2Freviews%2Fnew%3Fdraft%3D1');
			}
		}
	});
});

describe('requireDeveloper', () => {
	it('returns developers unchanged', () => {
		expect(requireDeveloper(eventFor(developer))).toBe(developer);
	});

	it('refuses signed-in users without the developer role', () => {
		try {
			requireDeveloper(eventFor(member));
			expect.unreachable('requireDeveloper should have failed');
		} catch (thrown) {
			expect(isHttpError(thrown)).toBe(true);
			if (isHttpError(thrown)) {
				expect(thrown.status).toBe(403);
			}
		}
	});

	it('redirects anonymous visitors instead of returning a 403', () => {
		try {
			requireDeveloper(eventFor(null));
			expect.unreachable('requireDeveloper should have redirected');
		} catch (thrown) {
			expect(isRedirect(thrown)).toBe(true);
		}
	});
});
