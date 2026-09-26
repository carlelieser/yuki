import { describe, expect, it, vi } from 'vitest';
import { isPlainClick, requestDownload, type ClickModifiers } from './download-request.ts';

const ENDPOINT = '/api/listings/acme-tools/download/v1.2.0?arch=arm64-v8a';

function respond(body: unknown, status = 200): typeof fetch {
	return vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status }));
}

describe('requestDownload', () => {
	it('returns the url the server resolved', async () => {
		const fetchImpl = respond({ url: 'https://example.com/app.apk' });

		expect(await requestDownload(ENDPOINT, fetchImpl)).toEqual({
			ok: true,
			url: 'https://example.com/app.apk'
		});
		expect(fetchImpl).toHaveBeenCalledWith(ENDPOINT, {
			headers: { accept: 'application/json' }
		});
	});

	it('passes on the reason the server gave for a failed download', async () => {
		const fetchImpl = respond({ message: 'No build for this architecture' }, 404);

		expect(await requestDownload(ENDPOINT, fetchImpl)).toEqual({
			ok: false,
			message: 'No build for this architecture'
		});
	});

	it('falls back to a general reason when the failure has no message', async () => {
		const fetchImpl = vi.fn().mockResolvedValue(new Response('Bad Gateway', { status: 502 }));

		expect(await requestDownload(ENDPOINT, fetchImpl)).toEqual({
			ok: false,
			message: 'Something went wrong. Try again.'
		});
	});

	it('asks the user to check their connection when the request never lands', async () => {
		const fetchImpl = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));

		expect(await requestDownload(ENDPOINT, fetchImpl)).toEqual({
			ok: false,
			message: 'Check your connection and try again.'
		});
	});
});

function click(overrides: Partial<ClickModifiers>): ClickModifiers {
	return { button: 0, metaKey: false, ctrlKey: false, shiftKey: false, ...overrides };
}

describe('isPlainClick', () => {
	it('takes over a plain left click', () => {
		expect(isPlainClick(click({ button: 0 }))).toBe(true);
	});

	it('leaves modified and middle clicks to the browser', () => {
		expect(isPlainClick(click({ button: 0, metaKey: true }))).toBe(false);
		expect(isPlainClick(click({ button: 0, ctrlKey: true }))).toBe(false);
		expect(isPlainClick(click({ button: 1 }))).toBe(false);
	});
});
