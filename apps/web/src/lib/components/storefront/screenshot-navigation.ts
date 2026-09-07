export function clampIndex(index: number, total: number): number {
	if (total <= 0) return 0;
	return Math.min(Math.max(index, 0), total - 1);
}

export function nextIndex(index: number, total: number): number {
	if (total <= 0) return 0;
	return (clampIndex(index, total) + 1) % total;
}

export function previousIndex(index: number, total: number): number {
	if (total <= 0) return 0;
	return (clampIndex(index, total) + total - 1) % total;
}

export function describeScreenshot(alt: string | null, index: number, total: number): string {
	return alt ?? `Screenshot ${index + 1} of ${total}`;
}
