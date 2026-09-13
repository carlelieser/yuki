import { describe, expect, it, vi } from 'vitest';
import { copyToClipboard, shareUrl } from './share-menu.ts';

describe('shareUrl', () => {
	it('builds an absolute url from the current page origin and path', () => {
		expect(shareUrl(new URL('https://yuki.app/listings/acme-tools'))).toBe(
			'https://yuki.app/listings/acme-tools'
		);
	});

	it('keeps a non standard port so local and preview hosts stay shareable', () => {
		expect(shareUrl(new URL('http://localhost:5173/listings/acme-tools'))).toBe(
			'http://localhost:5173/listings/acme-tools'
		);
	});

	it('drops the query string so tracking params are not shared', () => {
		expect(shareUrl(new URL('https://yuki.app/listings/acme-tools?arch=x64&ref=twitter'))).toBe(
			'https://yuki.app/listings/acme-tools'
		);
	});

	it('drops the hash so a deep linked section is not shared', () => {
		expect(shareUrl(new URL('https://yuki.app/listings/acme-tools#reviews'))).toBe(
			'https://yuki.app/listings/acme-tools'
		);
	});

	it('preserves an escaped slug', () => {
		expect(shareUrl(new URL('https://yuki.app/listings/acme%20tools'))).toBe(
			'https://yuki.app/listings/acme%20tools'
		);
	});
});

describe('copyToClipboard', () => {
	it('reports success and writes the value when the clipboard is available', async () => {
		const writeText = vi.fn().mockResolvedValue(undefined);

		const result = await copyToClipboard('https://yuki.app/listings/acme-tools', {
			writeText
		} as unknown as Clipboard);

		expect(result).toEqual({ ok: true });
		expect(writeText).toHaveBeenCalledWith('https://yuki.app/listings/acme-tools');
	});

	it('reports failure when the clipboard api is missing in an insecure context', async () => {
		expect(await copyToClipboard('https://yuki.app/listings/acme-tools', undefined)).toEqual({
			ok: false
		});
	});

	it('reports failure when the clipboard object has no writeText', async () => {
		expect(
			await copyToClipboard('https://yuki.app/listings/acme-tools', {} as unknown as Clipboard)
		).toEqual({ ok: false });
	});

	it('reports failure when the write is rejected by permissions', async () => {
		const writeText = vi.fn().mockRejectedValue(new Error('NotAllowedError'));

		const result = await copyToClipboard('https://yuki.app/listings/acme-tools', {
			writeText
		} as unknown as Clipboard);

		expect(result).toEqual({ ok: false });
	});
});
