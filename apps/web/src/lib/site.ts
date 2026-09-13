import { PUBLIC_SITE_URL } from '$env/static/public';

export const SITE_NAME = 'Yuki';
export const SITE_TAGLINE = 'Open-source Android apps, curated from GitHub';

export function siteUrl(pathname: string): string {
	return new URL(pathname, PUBLIC_SITE_URL).toString();
}
