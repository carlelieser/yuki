export const MAX_SCREENSHOTS = 8;

export type ReadmeImage = {
	url: string;
	alt: string | null;
	position: number;
};

const MARKDOWN_IMAGE = /!\[([^\]]*)\]\(\s*<?([^\s)>]+)>?(?:\s+["'][^"']*["'])?\s*\)/g;
const HTML_IMAGE = /<img\b[^>]*?\bsrc\s*=\s*["']([^"']+)["'][^>]*>/gi;
const HTML_IMAGE_ALT = /\balt\s*=\s*["']([^"']*)["']/i;

const BADGE_HOSTS = ['img.shields.io', 'badgen.net', 'badge.fury.io', 'travis-ci', 'codecov.io'];
const PREFERRED_SEGMENTS = ['screenshot', 'screenshots', 'preview', 'previews'];

function isRejected(url: string): boolean {
	const lowered = url.toLowerCase();
	if (lowered.endsWith('.svg')) return true;
	if (lowered.includes('badge')) return true;
	return BADGE_HOSTS.some((host) => lowered.includes(host));
}

function isPreferred(url: string): boolean {
	const lowered = url.toLowerCase();
	return PREFERRED_SEGMENTS.some((segment) => lowered.includes(segment));
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

export function extractReadmeImages(
	markdown: string,
	owner: string,
	name: string,
	defaultBranch: string
): ReadmeImage[] {
	const resolved: { url: string; alt: string | null }[] = [];
	const seen = new Set<string>();

	for (const candidate of collectCandidates(markdown)) {
		if (isRejected(candidate.url)) continue;

		const url = resolveImageUrl(candidate.url, owner, name, defaultBranch);
		if (url === null || isRejected(url) || seen.has(url)) continue;

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
