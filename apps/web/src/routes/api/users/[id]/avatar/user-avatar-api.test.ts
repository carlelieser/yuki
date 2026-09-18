import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';

const { getAvatar } = vi.hoisted(() => ({ getAvatar: vi.fn() }));

vi.mock('$lib/server/avatars.ts', () => ({ getAvatar }));

const { GET: readAvatar } = await import('./+server.ts');

const db = {} as Database;
const UPDATED_AT = new Date('2026-01-01T00:00:00.000Z');
const ETAG = `"${UPDATED_AT.getTime()}"`;

function request(ifNoneMatch?: string) {
	const headers = ifNoneMatch === undefined ? undefined : { 'if-none-match': ifNoneMatch };

	return {
		locals: { db },
		params: { id: 'user-1' },
		request: new Request('http://localhost/api/users/user-1/avatar', { headers })
	} as unknown as RequestEvent;
}

beforeEach(() => {
	getAvatar.mockReset();
	getAvatar.mockResolvedValue({
		contentType: 'image/png',
		bytes: Buffer.from([1, 2, 3, 4]),
		updatedAt: UPDATED_AT
	});
});

describe('GET /api/users/[id]/avatar', () => {
	it('serves the stored image with its own content type', async () => {
		const response = await readAvatar(request());

		expect(response.status).toBe(200);
		expect(response.headers.get('content-type')).toBe('image/png');
		expect(response.headers.get('etag')).toBe(ETAG);
		await expect(response.arrayBuffer()).resolves.toHaveProperty('byteLength', 4);
	});

	it('answers a matching etag without resending the bytes', async () => {
		const response = await readAvatar(request(ETAG));

		expect(response.status).toBe(304);
		expect(response.headers.get('etag')).toBe(ETAG);
	});

	it('resends the image when the caller holds a stale etag', async () => {
		const response = await readAvatar(request('"0"'));

		expect(response.status).toBe(200);
	});

	it('reports a missing picture so clients can fall back to initials', async () => {
		getAvatar.mockResolvedValue(null);

		try {
			await readAvatar(request());
			expect.unreachable('the handler should have failed');
		} catch (thrown) {
			expect(isHttpError(thrown)).toBe(true);
			if (isHttpError(thrown)) expect(thrown.status).toBe(404);
		}
	});
});
