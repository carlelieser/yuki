import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import { APIError } from 'better-auth/api';
import type { RequestEvent } from './$types';
import type { SessionUser } from '@yuki/auth';

const { getAuth, updateUser } = vi.hoisted(() => {
	const updateUser = vi.fn();
	return { getAuth: vi.fn(() => ({ api: { updateUser } })), updateUser };
});

vi.mock('$lib/server/auth.ts', () => ({ getAuth }));

const { POST: saveArchitecture } = await import('./+server.ts');

const user = { id: 'user-1' } as SessionUser;

function event(body: unknown, signedIn = true) {
	return {
		locals: { user: signedIn ? user : null },
		request: new Request('http://localhost/api/preferences/architecture', {
			method: 'POST',
			body: typeof body === 'string' ? body : JSON.stringify(body)
		})
	} as unknown as RequestEvent;
}

async function expectStatus(request: RequestEvent, status: number): Promise<void> {
	try {
		await saveArchitecture(request);
		expect.unreachable('the handler should have failed');
	} catch (thrown) {
		expect(isHttpError(thrown)).toBe(true);
		if (isHttpError(thrown)) expect(thrown.status).toBe(status);
	}
}

describe('POST /api/preferences/architecture', () => {
	beforeEach(() => {
		updateUser.mockReset();
	});

	it('saves a chosen architecture to the user', async () => {
		updateUser.mockResolvedValue({});

		const response = await saveArchitecture(event({ architecture: 'arm64-v8a' }));

		expect(updateUser).toHaveBeenCalledWith(
			expect.objectContaining({ body: { architecture: 'arm64-v8a' } })
		);
		await expect(response.json()).resolves.toEqual({ architecture: 'arm64-v8a' });
	});

	it('clears the preference when the user returns to the default', async () => {
		updateUser.mockResolvedValue({});

		const response = await saveArchitecture(event({ architecture: null }));

		expect(updateUser).toHaveBeenCalledWith(
			expect.objectContaining({ body: { architecture: null } })
		);
		await expect(response.json()).resolves.toEqual({ architecture: null });
	});

	it('refuses an anonymous request so preferences cannot be set for others', async () => {
		await expectStatus(event({ architecture: 'x86' }, false), 401);
	});

	it('rejects an architecture the app does not ship', async () => {
		await expectStatus(event({ architecture: 'mips' }), 400);
		expect(updateUser).not.toHaveBeenCalled();
	});

	it('rejects a body that is not shaped like a preference', async () => {
		await expectStatus(event('not json'), 400);
		await expectStatus(event({ architecture: 42 }), 400);
	});

	it('reports a rejected write instead of claiming it saved', async () => {
		updateUser.mockRejectedValue(new APIError('BAD_REQUEST', { message: 'nope' }));

		await expectStatus(event({ architecture: 'x86' }), 400);
	});
});
