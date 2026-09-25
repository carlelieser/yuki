import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';

const { findAvatarUrl } = vi.hoisted(() => ({ findAvatarUrl: vi.fn() }));

vi.mock('$lib/server/avatars.ts', () => ({ findAvatarUrl }));

const { GET: readAvatar } = await import('./+server.ts');

const db = {} as Database;
const AVATAR_URL = 'https://assets.yuki.test/avatars/user-1/a.webp';

function request() {
	return {
		locals: { db },
		params: { id: 'user-1' },
		request: new Request('http://localhost/api/users/user-1/avatar')
	} as unknown as RequestEvent;
}

beforeEach(() => {
	findAvatarUrl.mockReset();
	findAvatarUrl.mockResolvedValue(AVATAR_URL);
});

describe('GET /api/users/[id]/avatar', () => {
	it('redirects to the avatar in object storage', async () => {
		const response = await readAvatar(request());

		expect(findAvatarUrl).toHaveBeenCalledWith(db, 'user-1');
		expect(response.status).toBe(302);
		expect(response.headers.get('location')).toBe(AVATAR_URL);
	});

	it('keeps clients from caching the redirect so a new picture shows up', async () => {
		const response = await readAvatar(request());

		expect(response.headers.get('cache-control')).toBe('private, max-age=0, must-revalidate');
	});

	it('reports a missing picture so clients can fall back to initials', async () => {
		findAvatarUrl.mockResolvedValue(null);

		try {
			await readAvatar(request());
			expect.unreachable('the handler should have failed');
		} catch (thrown) {
			expect(isHttpError(thrown)).toBe(true);
			if (isHttpError(thrown)) expect(thrown.status).toBe(404);
		}
	});
});
