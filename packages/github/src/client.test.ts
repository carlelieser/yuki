import { describe, expect, it } from 'vitest';
import { GithubSkip, createGithubClient, type FetchImpl } from './client.ts';

type Call = { url: string; headers: Headers };

function recorder(responses: Response[]): { fetchImpl: FetchImpl; calls: Call[] } {
	const calls: Call[] = [];
	const queue = [...responses];

	const fetchImpl: FetchImpl = async (url, init) => {
		calls.push({ url, headers: new Headers(init?.headers) });
		const next = queue.shift();
		if (!next) throw new Error(`unexpected request to ${url}`);
		return next;
	};

	return { fetchImpl, calls };
}

function jsonResponse(body: unknown, init: ResponseInit = {}): Response {
	return new Response(JSON.stringify(body), {
		status: 200,
		headers: { 'content-type': 'application/json' },
		...init
	});
}

const noWait = async () => {};

describe('createGithubClient', () => {
	it('sends If-None-Match when an etag exists', async () => {
		const { fetchImpl, calls } = recorder([jsonResponse({ id: 1 })]);
		const client = createGithubClient('token', fetchImpl, noWait);

		await client.getRepository('acme', 'app', 'W/"abc123"');

		expect(calls[0]?.headers.get('if-none-match')).toBe('W/"abc123"');
	});

	it('omits If-None-Match when no etag is known', async () => {
		const { fetchImpl, calls } = recorder([jsonResponse({ id: 1 })]);
		const client = createGithubClient('token', fetchImpl, noWait);

		await client.getRepository('acme', 'app');

		expect(calls[0]?.headers.has('if-none-match')).toBe(false);
	});

	it('reports 304 as an explicit not-modified outcome, not an empty body', async () => {
		const { fetchImpl } = recorder([new Response(null, { status: 304 })]);
		const client = createGithubClient('token', fetchImpl, noWait);

		const response = await client.getRepository('acme', 'app', 'W/"abc123"');

		expect(response).toEqual({ isModified: false });
		expect(client.stats.notModifiedCount).toBe(1);
	});

	it('returns the body and etag on a fresh response', async () => {
		const { fetchImpl } = recorder([
			jsonResponse({ id: 7 }, { headers: { etag: 'W/"new"', 'content-type': 'application/json' } })
		]);
		const client = createGithubClient('token', fetchImpl, noWait);

		const response = await client.getRepository('acme', 'app');

		expect(response).toEqual({ isModified: true, body: { id: 7 }, etag: 'W/"new"' });
	});

	it('retries a code-search 404 instead of reporting no results', async () => {
		const { fetchImpl, calls } = recorder([
			new Response(null, { status: 404 }),
			jsonResponse({ total_count: 1, incomplete_results: false, items: [] })
		]);
		const client = createGithubClient('token', fetchImpl, noWait);

		const response = await client.searchCode('rikka.shizuku.ShizukuProvider', 1, 100);

		expect(calls).toHaveLength(2);
		expect(response).toMatchObject({ isModified: true });
	});

	it('skips a deleted repository without failing the run', async () => {
		const { fetchImpl } = recorder([new Response(null, { status: 404 })]);
		const client = createGithubClient('token', fetchImpl, noWait);

		await expect(client.getRepository('acme', 'gone')).rejects.toBeInstanceOf(GithubSkip);
	});

	it('reads the readme as raw text', async () => {
		const { fetchImpl, calls } = recorder([
			new Response('# Title', { status: 200, headers: { etag: 'W/"r"' } })
		]);
		const client = createGithubClient('token', fetchImpl, noWait);

		const response = await client.getReadme('acme', 'app');

		expect(calls[0]?.headers.get('accept')).toBe('application/vnd.github.raw');
		expect(response).toMatchObject({ isModified: true, body: '# Title' });
	});

	it('counts every request it makes', async () => {
		const { fetchImpl } = recorder([jsonResponse({ id: 1 }), new Response(null, { status: 304 })]);
		const client = createGithubClient('token', fetchImpl, noWait);

		await client.getRepository('acme', 'app');
		await client.getRepository('acme', 'app', 'W/"abc"');

		expect(client.stats).toEqual({ requestCount: 2, notModifiedCount: 1 });
	});

	it('authenticates every request', async () => {
		const { fetchImpl, calls } = recorder([jsonResponse({ id: 1 })]);
		const client = createGithubClient('secret-token', fetchImpl, noWait);

		await client.getRepository('acme', 'app');

		expect(calls[0]?.headers.get('authorization')).toBe('Bearer secret-token');
	});
});
