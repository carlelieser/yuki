export function attribute(source: string, name: string): string | null {
	const match = source.match(new RegExp(`android:${name}="([^"]*)"`));
	return match?.[1] ?? null;
}

export function numeric(source: string, name: string, fallback: number): number {
	const raw = attribute(source, name);
	if (raw === null) return fallback;

	const parsed = Number.parseFloat(raw.replace(/(dp|dip|px|sp)$/, ''));
	return Number.isFinite(parsed) ? parsed : fallback;
}

export function escapeXml(value: string): string {
	return value
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;');
}
