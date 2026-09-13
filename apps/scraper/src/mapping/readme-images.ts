export const MAX_SCREENSHOTS = 8;

export type ReadmeImage = {
	url: string;
	alt: string | null;
	position: number;
};

const MARKDOWN_IMAGE = /!\[([^\]]*)\]\(\s*<?([^\s)>]+)>?(?:\s+["'][^"']*["'])?\s*\)/g;
const HTML_IMAGE = /<img\b[^>]*?\bsrc\s*=\s*["']([^"']+)["'][^>]*>/gi;
const HTML_IMAGE_ALT = /\balt\s*=\s*["']([^"']*)["']/i;

const BADGE_HOSTS = [
	'img.shields.io',
	'badgen.net',
	'badge.fury.io',
	'travis-ci',
	'codecov.io',
	'discordapp.com/api',
	'discord.com/api'
];
const PREFERRED_WORDS = ['screenshot', 'screenshots', 'preview', 'previews'];
const BANNER_WORDS = ['banner', 'hero', 'cover', 'header', 'splash'];
const CHROME_WORDS = [
	'icon',
	'logo',
	'avatar',
	'button',
	'arrow',
	'divider',
	'spacer',
	'bullet',
	'launcher'
];
const STORE_WORDS = [
	'izzyondroid',
	'fdroid',
	'droidify',
	'obtainium',
	'playstore',
	'googleplay',
	'amazonappstore',
	'kofi',
	'buymeacoffee',
	'patreon',
	'paypal',
	'liberapay',
	'opencollective'
];
const STOREFRONT_WORDS = ['github', 'gitlab', 'codeberg', 'sourceforge'];
const SOCIAL_WORDS = ['tg', 'telegram', 'discord', 'matrix', 'slack', 'mastodon', 'twitter', 'qq'];
const GROUP_WORDS = ['group', 'chat', 'join', 'channel', 'invite', 'community', 'qr'];
const RASTER_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.webp', '.gif', '.avif'];

function pathOf(url: string): string {
	const lowered = url.toLowerCase();
	const withoutFragment = lowered.split('#')[0] ?? '';
	return withoutFragment.split('?')[0] ?? '';
}

function wordsOf(path: string): string[] {
	const filename = path.split('/').pop() ?? '';
	const stem = filename.replace(/\.[a-z0-9]+$/, '');
	return stem.split(/[^a-z]+/).filter((word) => word !== '');
}

function hasWord(path: string, words: string[]): boolean {
	const found = wordsOf(path);
	return words.some((word) => found.includes(word));
}

function hasWordInPath(path: string, words: string[]): boolean {
	const found = path.split(/[^a-z]+/).filter((part) => part !== '');
	return words.some((word) => found.includes(word));
}

function isRasterImage(path: string): boolean {
	return RASTER_EXTENSIONS.some((extension) => path.endsWith(extension));
}

function isSocialInvite(path: string): boolean {
	const words = wordsOf(path);
	const isSocial = SOCIAL_WORDS.some((word) => words.includes(word));
	return isSocial && GROUP_WORDS.some((word) => words.includes(word));
}

function isStoreBadge(path: string): boolean {
	const filename = path.split('/').pop() ?? '';
	const collapsed = filename.replace(/[^a-z]/g, '');
	return STORE_WORDS.some((word) => collapsed.includes(word));
}

function isGetItOnButton(path: string): boolean {
	const words = wordsOf(path);
	return words.includes('get') && words.includes('on');
}

function isStorefrontName(path: string): boolean {
	return hasWord(path, STOREFRONT_WORDS) && wordsOf(path).length === 1;
}

function isRejected(url: string): boolean {
	const lowered = url.toLowerCase();
	const path = pathOf(url);
	if (!isRasterImage(path)) return true;
	if (lowered.includes('badge') || lowered.includes('shield')) return true;
	if (/\/api\//.test(lowered)) return true;
	if (hasWord(path, CHROME_WORDS)) return true;
	if (isStoreBadge(path)) return true;
	if (isGetItOnButton(path)) return true;
	if (isStorefrontName(path)) return true;
	if (isSocialInvite(path)) return true;
	return BADGE_HOSTS.some((host) => lowered.includes(host));
}

function isPreferred(url: string): boolean {
	return hasWordInPath(pathOf(url), PREFERRED_WORDS);
}

function isBanner(url: string): boolean {
	const path = pathOf(url);
	if (hasWord(path, CHROME_WORDS)) return false;
	return hasWord(path, BANNER_WORDS);
}

export function resolveImageUrl(
	rawUrl: string,
	owner: string,
	name: string,
	defaultBranch: string
): string | null {
	const url = rawUrl.trim();
	if (url === '' || url.startsWith('data:') || url.startsWith('#')) return null;

	if (url.startsWith('http://') || url.startsWith('https://')) return url;
	if (url.startsWith('//')) return `https:${url}`;

	const path = url.replace(/^\.\//, '').replace(/^\//, '');
	if (path === '') return null;

	return `https://raw.githubusercontent.com/${owner}/${name}/${defaultBranch}/${path}`;
}

function collectCandidates(markdown: string): { url: string; alt: string | null }[] {
	const candidates: { url: string; alt: string | null }[] = [];

	for (const match of markdown.matchAll(MARKDOWN_IMAGE)) {
		const alt = match[1]?.trim() ?? '';
		const url = match[2];
		if (url) candidates.push({ url, alt: alt === '' ? null : alt });
	}

	for (const match of markdown.matchAll(HTML_IMAGE)) {
		const url = match[1];
		if (!url) continue;
		const alt = match[0].match(HTML_IMAGE_ALT)?.[1]?.trim() ?? '';
		candidates.push({ url, alt: alt === '' ? null : alt });
	}

	return candidates;
}

export function findBannerUrl(
	markdown: string,
	owner: string,
	name: string,
	defaultBranch: string
): string | null {
	for (const candidate of collectCandidates(markdown)) {
		if (isRejected(candidate.url)) continue;

		const url = resolveImageUrl(candidate.url, owner, name, defaultBranch);
		if (url === null || isRejected(url)) continue;
		if (isBanner(url)) return url;
	}

	return null;
}

export function extractReadmeImages(
	markdown: string,
	owner: string,
	name: string,
	defaultBranch: string,
	exclude: string | null = null
): ReadmeImage[] {
	const resolved: { url: string; alt: string | null }[] = [];
	const seen = new Set<string>();

	for (const candidate of collectCandidates(markdown)) {
		if (isRejected(candidate.url)) continue;

		const url = resolveImageUrl(candidate.url, owner, name, defaultBranch);
		if (url === null || isRejected(url) || seen.has(url) || url === exclude) continue;

		seen.add(url);
		resolved.push({ url, alt: candidate.alt });
	}

	const preferred = resolved.filter((image) => isPreferred(image.url));
	const chosen = preferred.length > 0 ? preferred : resolved;

	return chosen.slice(0, MAX_SCREENSHOTS).map((image, index) => ({
		url: image.url,
		alt: image.alt,
		position: index
	}));
}
