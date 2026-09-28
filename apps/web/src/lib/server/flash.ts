import { redirect, type Cookies } from '@sveltejs/kit';

const FLASH_MAX_AGE_SECONDS = 60;

type FlashEvent = { url: URL; cookies: Cookies };

function cookieName(param: string): string {
	return `flash_${param}`;
}

export function takeQueryFlash(event: FlashEvent, param: string): string | null {
	const { url, cookies } = event;
	const value = url.searchParams.get(param);
	const options = { path: url.pathname, httpOnly: true, sameSite: 'lax' as const };

	if (value !== null) {
		cookies.set(cookieName(param), value, { ...options, maxAge: FLASH_MAX_AGE_SECONDS });
		const clean = new URL(url);
		clean.searchParams.delete(param);
		redirect(303, `${clean.pathname}${clean.search}`);
	}

	const flashed = cookies.get(cookieName(param)) ?? null;
	if (flashed !== null) cookies.delete(cookieName(param), options);
	return flashed;
}
