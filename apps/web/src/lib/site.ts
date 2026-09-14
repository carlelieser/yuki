import { env } from '$env/dynamic/public';

export const SITE_NAME = 'Yuki';
export const SITE_TAGLINE = 'Shizuku-powered Android apps, all in one place';

const FALLBACK_ORIGIN = 'http://localhost:5173';

export function siteOrigin(): string {
	return env.PUBLIC_SITE_URL ?? FALLBACK_ORIGIN;
}

export function siteUrl(pathname: string): string {
	return new URL(pathname, siteOrigin()).toString();
}
