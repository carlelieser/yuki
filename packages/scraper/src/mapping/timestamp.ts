export function parseTimestamp(value: string | null | undefined): Date | null {
	if (value === null || value === undefined) return null;

	const parsed = new Date(value);
	return Number.isNaN(parsed.getTime()) ? null : parsed;
}
