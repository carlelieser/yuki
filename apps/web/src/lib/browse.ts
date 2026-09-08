export const BROWSE_PAGE_SIZE = 24;
export const MAX_BROWSE_OFFSET = 2000;

export function readBrowseOffset(raw: string | null): number {
	const parsed = Number(raw);
	if (!Number.isFinite(parsed) || parsed <= 0) return 0;
	return Math.min(Math.floor(parsed), MAX_BROWSE_OFFSET);
}
