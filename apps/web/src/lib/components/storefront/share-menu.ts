export function shareUrl(pageUrl: URL): string {
	const url = new URL(pageUrl.pathname, pageUrl.origin);
	return url.toString();
}

export type CopyResult = { ok: true } | { ok: false };

export async function copyToClipboard(
	value: string,
	clipboard: Clipboard | undefined
): Promise<CopyResult> {
	if (clipboard === undefined || typeof clipboard.writeText !== 'function') return { ok: false };

	try {
		await clipboard.writeText(value);
		return { ok: true };
	} catch {
		return { ok: false };
	}
}
