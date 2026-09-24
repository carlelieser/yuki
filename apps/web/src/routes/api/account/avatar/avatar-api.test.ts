import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';
import type { SessionUser } from '@yuki/auth';

const { getAuth, updateUser, putObject, deleteObject } = vi.hoisted(() => {
	const updateUser = vi.fn();
	return {
		getAuth: vi.fn(() => ({ api: { updateUser } })),
		updateUser,
		putObject: vi.fn(),
		deleteObject: vi.fn()
	};
});

vi.mock('$lib/server/auth.ts', () => ({ getAuth }));
vi.mock('$lib/server/object-storage.ts', async (importOriginal) => ({
	...(await importOriginal<typeof import('$lib/server/object-storage.ts')>()),
	putObject,
	deleteObject
}));

const { POST: uploadAvatar } = await import('./+server.ts');
const { MAX_AVATAR_BYTES } = await import('$lib/server/avatars.ts');

const ASSETS_BASE_URL = 'https://assets.yuki.test';
const AVATAR_URL = new RegExp(`^${ASSETS_BASE_URL}/avatars/user-1/[0-9a-f-]{36}\\.png$`);
const PREVIOUS_AVATAR = `${ASSETS_BASE_URL}/avatars/user-1/previous.png`;

const db = {} as Database;
let user: SessionUser;

function upload(body: unknown, signedIn = true) {
	return {
		locals: { db, user: signedIn ? user : null },
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

async function uploadedImage(response: Response): Promise<string> {
	const { image } = (await response.json()) as { image: string };
	return image;
}

beforeEach(() => {
	vi.stubEnv('ASSETS_BASE_URL', ASSETS_BASE_URL);
	user = { id: 'user-1', image: null } as SessionUser;
	updateUser.mockReset();
	putObject.mockReset();
	deleteObject.mockReset();
});

describe('POST /api/account/avatar', () => {
	it('points the profile at the public asset url of the uploaded picture', async () => {
		updateUser.mockResolvedValue({});

		const image = await uploadedImage(await uploadAvatar(upload(imageOf('image/png'))));

		expect(image).toMatch(AVATAR_URL);
		expect(putObject).toHaveBeenCalledWith(
			image.slice(`${ASSETS_BASE_URL}/`.length),
			expect.any(Buffer),
			'image/png'
		);
		expect(updateUser).toHaveBeenCalledWith(expect.objectContaining({ body: { image } }));
	});

	it('uses a fresh url for every upload so cached copies never go stale', async () => {
		updateUser.mockResolvedValue({});

		const first = await uploadedImage(await uploadAvatar(upload(imageOf('image/png'))));
		const second = await uploadedImage(await uploadAvatar(upload(imageOf('image/png'))));

		expect(first).not.toBe(second);
	});

	it('deletes the previous stored picture once the profile points at the new one', async () => {
		updateUser.mockResolvedValue({});
		user = { ...user, image: PREVIOUS_AVATAR };

		await uploadAvatar(upload(imageOf('image/png')));

		expect(deleteObject).toHaveBeenCalledExactlyOnceWith('avatars/user-1/previous.png');
	});

	it('leaves a picture hosted elsewhere untouched', async () => {
		updateUser.mockResolvedValue({});
		user = { ...user, image: 'https://avatars.githubusercontent.com/u/1' };

		await uploadAvatar(upload(imageOf('image/png')));

		expect(deleteObject).not.toHaveBeenCalled();
	});

	it('never deletes a stored asset that is not an avatar', async () => {
		updateUser.mockResolvedValue({});
		user = { ...user, image: `${ASSETS_BASE_URL}/icons/listing.png` };

		await uploadAvatar(upload(imageOf('image/png')));

		expect(deleteObject).not.toHaveBeenCalled();
	});

	it('refuses an anonymous upload so a picture cannot be set for others', async () => {
		await expectStatus(upload(imageOf('image/png'), false), 401);
		expect(putObject).not.toHaveBeenCalled();
	});

	it('rejects an image type the app will not serve', async () => {
		await expectStatus(upload(imageOf('image/gif')), 400);
		expect(putObject).not.toHaveBeenCalled();
	});

	it('rejects an image past the size ceiling', async () => {
		await expectStatus(upload(imageOf('image/png', MAX_AVATAR_BYTES + 1)), 400);
		expect(putObject).not.toHaveBeenCalled();
	});

	it('rejects a request carrying no image', async () => {
		await expectStatus(upload({}), 400);
		await expectStatus(upload('not json'), 400);
		await expectStatus(upload({ contentType: 'image/png' }), 400);
		expect(putObject).not.toHaveBeenCalled();
	});

	it('rejects data that is not base64 so a broken body cannot be uploaded', async () => {
		await expectStatus(upload({ contentType: 'image/png', data: 'not base64!!' }), 400);
		expect(putObject).not.toHaveBeenCalled();
	});

	it('rejects an empty image', async () => {
		await expectStatus(upload({ contentType: 'image/png', data: '' }), 400);
		expect(putObject).not.toHaveBeenCalled();
	});

	it('uploads the decoded bytes rather than the base64 text', async () => {
		updateUser.mockResolvedValue({});
		const bytes = Buffer.from([1, 2, 3, 4]);

		await uploadAvatar(upload({ contentType: 'image/webp', data: bytes.toString('base64') }));

		expect(putObject).toHaveBeenCalledWith(expect.any(String), bytes, 'image/webp');
	});

	it('reports a rejected profile write instead of claiming it saved', async () => {
		updateUser.mockRejectedValue(new APIError('BAD_REQUEST', { message: 'nope' }));
		user = { ...user, image: PREVIOUS_AVATAR };

		await expectStatus(upload(imageOf('image/png')), 400);

		const [uploadedKey] = putObject.mock.calls[0] as [string];
		expect(deleteObject).toHaveBeenCalledExactlyOnceWith(uploadedKey);
	});
});
