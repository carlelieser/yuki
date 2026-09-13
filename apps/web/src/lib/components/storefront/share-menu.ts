import { encode } from 'uqr';

const QUIET_ZONE_MODULES = 4;

export function shareUrl(pageUrl: URL): string {
	const url = new URL(pageUrl.pathname, pageUrl.origin);
	return url.toString();
}

export type QrCode = { size: number; path: string };

export function qrCodeFor(value: string): QrCode {
	const { size, data } = encode(value, { ecc: 'M', border: QUIET_ZONE_MODULES });

	const path = data
		.flatMap((row, y) =>
			row
				.map((isDark, x) => (isDark ? `M${x},${y}h1v1h-1z` : ''))
				.filter((segment) => segment !== '')
		)
		.join('');

	return { size, path };
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
