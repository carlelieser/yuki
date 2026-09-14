export type SitemapEntry = {
	path: string;
	lastmod?: Date | null;
};

const ESCAPES: Record<string, string> = {
	'&': '&amp;',
	'<': '&lt;',
	'>': '&gt;',
	'"': '&quot;',
	"'": '&apos;'
};

function escapeXml(value: string): string {
	return value.replace(/[&<>"']/g, (character) => ESCAPES[character] ?? character);
}

function toIsoDate(lastmod: Date): string {
	return lastmod.toISOString().slice(0, 10);
}

function toUrlElement(entry: SitemapEntry, toAbsolute: (path: string) => string): string {
	const location = `\t\t<loc>${escapeXml(toAbsolute(entry.path))}</loc>`;
	if (!entry.lastmod) return `\t<url>\n${location}\n\t</url>`;

	return `\t<url>\n${location}\n\t\t<lastmod>${toIsoDate(entry.lastmod)}</lastmod>\n\t</url>`;
}

export function toSitemapXml(
	entries: SitemapEntry[],
	toAbsolute: (path: string) => string
): string {
	const urls = entries.map((entry) => toUrlElement(entry, toAbsolute)).join('\n');

	return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`;
}
