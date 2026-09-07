export const MAX_QUERY_LENGTH = 100;
export const MIN_TRIGRAM_LENGTH = 3;
export const MAX_SEARCH_OFFSET = 500;

const TOKEN_ALLOWED_CHARACTERS = /[^A-Za-z0-9._-]/g;

export function normalizeSearchQuery(raw: unknown): string {
	if (typeof raw !== 'string') return '';
	return raw.replace(/\s+/g, ' ').trim().slice(0, MAX_QUERY_LENGTH);
}

export function toPrefixTsQuery(normalized: string): string {
	const tokens = normalized
		.split(' ')
		.map((token) => token.replace(TOKEN_ALLOWED_CHARACTERS, ''))
		.filter((token) => token.length > 0);

	if (tokens.length === 0) return '';

	return tokens
		.map((token, index) => (index === tokens.length - 1 ? `${token}:*` : token))
		.join(' & ');
}

export function hasEnoughLengthForTrigram(normalized: string): boolean {
	return normalized.length >= MIN_TRIGRAM_LENGTH;
}

export function readOffset(raw: string | null): number {
	const parsed = Number(raw);
	if (!Number.isFinite(parsed) || parsed <= 0) return 0;
	return Math.min(Math.floor(parsed), MAX_SEARCH_OFFSET);
}
