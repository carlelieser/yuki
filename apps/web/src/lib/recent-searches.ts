import { normalizeSearchQuery } from './search-query.ts';

export const RECENT_SEARCHES_KEY = 'yuki:recent-searches';
export const MAX_RECENT_SEARCHES = 5;

export function parseRecentSearches(raw: string | null): string[] {
	if (raw === null) return [];

	let parsed: unknown;
	try {
		parsed = JSON.parse(raw);
	} catch {
		return [];
	}

	if (!Array.isArray(parsed)) return [];

	const terms = parsed.map((entry) => normalizeSearchQuery(entry)).filter((entry) => entry !== '');

	return dedupe(terms).slice(0, MAX_RECENT_SEARCHES);
}

export function addRecentSearch(existing: string[], raw: string): string[] {
	const term = normalizeSearchQuery(raw);
	if (term === '') return existing;

	return dedupe([term, ...existing]).slice(0, MAX_RECENT_SEARCHES);
}

export function removeRecentSearch(existing: string[], raw: string): string[] {
	const term = normalizeSearchQuery(raw);
	if (term === '') return existing;

	const key = term.toLowerCase();
	return existing.filter((entry) => entry.toLowerCase() !== key);
}

function dedupe(terms: string[]): string[] {
	const seen = new Set<string>();
	return terms.filter((term) => {
		const key = term.toLowerCase();
		if (seen.has(key)) return false;
		seen.add(key);
		return true;
	});
}
