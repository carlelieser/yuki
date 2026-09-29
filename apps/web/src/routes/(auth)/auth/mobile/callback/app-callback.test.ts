import { describe, expect, it, vi } from 'vitest';

const { load } = await import('./+page.server.ts');

type Event = Parameters<typeof load>[0];

describe('app return page', () => {
	it('opens the Yuki app with the same result', () => {
		const url = new URL('https://yukistore.org/auth/mobile/callback?token=t&state=s');
		const setHeaders = vi.fn();

		const data = load({ url, setHeaders } as unknown as Event);

		expect(data.appUrl).toBe(
			'intent://yukistore.org/auth/mobile/callback?token=t&state=s#Intent;scheme=https;package=app.yuki;end'
		);
	});

	it('keeps the ticket out of caches and referrers', () => {
		const url = new URL('https://yukistore.org/auth/mobile/callback?token=t&state=s');
		const setHeaders = vi.fn();

		load({ url, setHeaders } as unknown as Event);

		expect(setHeaders).toHaveBeenCalledWith({
			'cache-control': 'no-store',
			'referrer-policy': 'no-referrer'
		});
	});
});
