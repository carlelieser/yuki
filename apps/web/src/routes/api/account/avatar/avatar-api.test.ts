import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';
import type { SessionUser } from '@yuki/auth';

const { getAuth, updateUser, saveAvatar } = vi.hoisted(() => {
	const updateUser = vi.fn();
	return { getAuth: vi.fn(() => ({ api: { updateUser } })), updateUser, saveAvatar: vi.fn() };
});

vi.mock('$lib/server/auth.ts', () => ({ getAuth }));
vi.mock('$lib/server/avatars.ts', async (importOriginal) => ({
	...(await importOriginal<typeof import('$lib/server/avatars.ts')>()),
	saveAvatar
}));

const { POST: uploadAvatar } = await import('./+server.ts');
const { MAX_AVATAR_BYTES } = await import('$lib/server/avatars.ts');

const user = { id: 'user-1' } as SessionUser;
const db = {} as Database;
const UPDATED_AT = new Date('2026-01-01T00:00:00.000Z');

function upload(body: unknown, signedIn = true) {
	return {
		locals: { db, user: signedIn ? user : null },
		url: new URL('http://yuki.test/api/account/avatar'),
		request: new Request('http://yuki.test/api/account/avatar', {
			method: 'POST',
			headers: { 'content-type': 'application/json' },
			body: typeof body === 'string' ? body : JSON.stringify(body)
		})
	} as unknown as RequestEvent;
}

function imageOf(contentType: string, size = 16) {
	return { contentType, data: Buffer.from(new Uint8Array(size)).toString('base64') };
}

async function expectStatus(request: RequestEvent, status: number): Promise<void> {
	try {
		await uploadAvatar(request);
		expect.unreachable('the handler should have failed');
	} catch (thrown) {
		expect(isHttpError(thrown)).toBe(true);
		if (isHttpError(thrown)) expect(thrown.status).toBe(status);
	}
}

beforeEach(() => {
	updateUser.mockReset();
	saveAvatar.mockReset();
	saveAvatar.mockResolvedValue(UPDATED_AT);
});

describe('POST /api/account/avatar', () => {
	it('points the profile at an absolute url so native clients can load it', async () => {
		updateUser.mockResolvedValue({});

		const response = await uploadAvatar(upload(imageOf('image/png')));

		expect(saveAvatar).toHaveBeenCalledWith(
			db,
			expect.objectContaining({ userId: 'user-1', contentType: 'image/png' })
		);
		await expect(response.json()).resolves.toEqual({
			image: `http://yuki.test/api/users/user-1/avatar?v=${UPDATED_AT.getTime()}`
		});
	});

	it('refuses an anonymous upload so a picture cannot be set for others', async () => {
		await expectStatus(upload(imageOf('image/png'), false), 401);
		expect(saveAvatar).not.toHaveBeenCalled();
	});

	it('rejects an image type the app will not serve', async () => {
		await expectStatus(upload(imageOf('image/gif')), 400);
		expect(saveAvatar).not.toHaveBeenCalled();
	});

	it('rejects an image past the size ceiling', async () => {
		await expectStatus(upload(imageOf('image/png', MAX_AVATAR_BYTES + 1)), 400);
		expect(saveAvatar).not.toHaveBeenCalled();
	});

	it('rejects a request carrying no image', async () => {
		await expectStatus(upload({}), 400);
		await expectStatus(upload('not json'), 400);
		await expectStatus(upload({ contentType: 'image/png' }), 400);
		expect(saveAvatar).not.toHaveBeenCalled();
	});

	it('rejects data that is not base64 so a broken body cannot be stored', async () => {
		await expectStatus(upload({ contentType: 'image/png', data: 'not base64!!' }), 400);
		expect(saveAvatar).not.toHaveBeenCalled();
	});

	it('rejects an empty image', async () => {
		await expectStatus(upload({ contentType: 'image/png', data: '' }), 400);
		expect(saveAvatar).not.toHaveBeenCalled();
	});

	it('stores the decoded bytes rather than the base64 text', async () => {
		updateUser.mockResolvedValue({});
		const bytes = Buffer.from([1, 2, 3, 4]);

		await uploadAvatar(upload({ contentType: 'image/webp', data: bytes.toString('base64') }));

		expect(saveAvatar).toHaveBeenCalledWith(db, expect.objectContaining({ bytes }));
	});

	it('reports a rejected profile write instead of claiming it saved', async () => {
		updateUser.mockRejectedValue(new APIError('BAD_REQUEST', { message: 'nope' }));

		await expectStatus(upload(imageOf('image/png')), 400);
	});
});
