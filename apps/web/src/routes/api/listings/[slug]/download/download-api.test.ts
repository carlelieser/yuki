import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isHttpError } from '@sveltejs/kit';
import type { RequestEvent } from './[tag]/$types';

const { resolveDownload } = vi.hoisted(() => ({ resolveDownload: vi.fn() }));

vi.mock('$lib/server/download-target.ts', () => ({ resolveDownload }));

const { error } = await import('@sveltejs/kit');
const { GET: download } = await import('./[tag]/+server.ts');

const event = {
	locals: {},
	params: { slug: 'acme-tools', tag: 'v1.2.0' },
	url: new URL('http://localhost/api/listings/acme-tools/download/v1.2.0?arch=arm64-v8a')
} as unknown as RequestEvent;

beforeEach(() => {
	resolveDownload.mockReset();
});

describe('GET /api/listings/[slug]/download/[tag]', () => {
	it('answers with the resolved download url instead of redirecting', async () => {
		resolveDownload.mockResolvedValue('https://example.com/app-arm64-v8a.apk');

		const response = await download(event);

		expect(resolveDownload).toHaveBeenCalledWith(event);
		await expect(response.json()).resolves.toEqual({
			url: 'https://example.com/app-arm64-v8a.apk'
		});
	});

	it('passes a resolution failure through as an http error', async () => {
		resolveDownload.mockImplementation(() => error(502, 'GitHub unavailable'));

		const thrown = await Promise.resolve(download(event)).catch((failure: unknown) => failure);

		expect(isHttpError(thrown, 502)).toBe(true);
	});
});
