import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isRedirect } from '@sveltejs/kit';

const { getAuth, handler } = vi.hoisted(() => {
	const handler = vi.fn();
	const context = { createAuthCookie: (name: string) => ({ name: `better-auth.${name}` }) };
	return {
		handler,
		getAuth: vi.fn(() => ({ handler, $context: Promise.resolve(context) }))
	};
});

vi.mock('$lib/server/auth.ts', () => ({ getAuth }));

const { load, actions } = await import('./+page.server.ts');

type Event = Parameters<typeof load>[0];

const STATE = 'b'.repeat(43);

function routeCalls(routes: Record<string, () => Response>) {
	handler.mockImplementation(async (request: Request) => {
		const route = routes[new URL(request.url).pathname.replace('/api/auth', '')];
		return route ? route() : new Response(null, { status: 404 });
	});
}

function verified(token = 'session-1') {
	return () => Response.json({ session: { token }, user: { email: 'ada@yuki.test' } });
}

function event(query: string, body?: Record<string, string>) {
	const url = new URL(`https://yukistore.org/auth/mobile/github/link${query}`);
	const cookies = { set: vi.fn() };
	const request = new Request(url, {
		method: body ? 'POST' : 'GET',
		body: body ? new URLSearchParams(body) : undefined
	});
	return { url, request, cookies, getClientAddress: () => '203.0.113.9' } as unknown as Event;
}

async function redirectOf(run: () => unknown): Promise<URL> {
	try {
		await run();
	} catch (thrown) {
		if (isRedirect(thrown)) return new URL(thrown.location, 'https://yukistore.org');
		throw thrown;
	}
	return expect.unreachable('expected a redirect');
}

beforeEach(() => {
	process.env.BETTER_AUTH_URL = 'https://yukistore.org';
	handler.mockReset();
});

describe('link confirmation page', () => {
	it('shows which Yuki account GitHub will be connected to', async () => {
		routeCalls({
			'/one-time-token/verify': verified(),
			'/one-time-token/generate': () => Response.json({ token: 'confirm-1' })
		});

		const data = await load(event(`?ticket=ticket-1&state=${STATE}`));

		expect(data).toMatchObject({ email: 'ada@yuki.test', confirmTicket: 'confirm-1' });
	});

	it('sends the app an error when the ticket has expired', async () => {
		routeCalls({ '/one-time-token/verify': () => new Response(null, { status: 400 }) });

		const target = await redirectOf(() => load(event(`?ticket=old&state=${STATE}`)));

		expect(Object.fromEntries(target.searchParams)).toEqual({
			error: 'link_expired',
			state: STATE
		});
	});
});

describe('continuing to GitHub', () => {
	it('starts the link for the ticket owner and keeps their session out of the browser', async () => {
		routeCalls({
			'/one-time-token/verify': verified(),
			'/link-social': () =>
				new Response(JSON.stringify({ url: 'https://github.com/login/oauth/authorize' }), {
					headers: [
						['content-type', 'application/json'],
						['set-cookie', 'better-auth.state=signed-state; Path=/; HttpOnly; Max-Age=300'],
						['set-cookie', 'better-auth.session_data=cache; Path=/; HttpOnly']
					]
				})
		});
		const request = event('', { state: STATE, confirmTicket: 'confirm-1' });

		const target = await redirectOf(() => actions.default(request));

		expect(target.href).toBe('https://github.com/login/oauth/authorize');
		const [linkCall] = handler.mock.calls[1] as [Request];
		expect(linkCall.headers.get('authorization')).toBe('Bearer session-1');
		expect(request.cookies.set).toHaveBeenCalledTimes(1);
		expect(request.cookies.set).toHaveBeenCalledWith(
			'better-auth.state',
			'signed-state',
			expect.objectContaining({ httpOnly: true })
		);
	});
});
