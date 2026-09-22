import { parseGradient, type VectorGradient } from './vector-gradient.ts';

const MAX_COLOR_HOPS = 8;

const FRAMEWORK_COLORS = new Map([
	['white', '#FFFFFFFF'],
	['black', '#FF000000'],
	['transparent', '#00000000'],
	['background_light', '#FFFFFFFF'],
	['background_dark', '#FF000000'],
	['primary_text_light', '#FF000000'],
	['primary_text_dark', '#FFFFFFFF'],
	['secondary_text_light', '#FF666666'],
	['secondary_text_dark', '#FFBEBEBE'],
	['holo_blue_light', '#FF33B5E5'],
	['holo_blue_dark', '#FF0099CC'],
	['holo_blue_bright', '#FF00DDFF'],
	['holo_green_light', '#FF99CC00'],
	['holo_green_dark', '#FF669900'],
	['holo_red_light', '#FFFF4444'],
	['holo_red_dark', '#FFCC0000'],
	['holo_orange_light', '#FFFFBB33'],
	['holo_orange_dark', '#FFFF8800'],
	['holo_purple', '#FFAA66CC'],
	['darker_gray', '#FFAAAAAA'],
	['background_holo_dark', '#FF000000'],
	['background_holo_light', '#FFFFFFFF']
]);

export function resolveColor(raw: string | null, colors: Map<string, string>): string | null {
	if (raw === null) return null;

	let value: string | null = raw;
	const seen = new Set<string>();

	for (let hop = 0; value !== null && value.startsWith('@'); hop += 1) {
		if (hop >= MAX_COLOR_HOPS || seen.has(value)) return null;
		seen.add(value);

		const reference: string = value;
		const isFramework = reference.startsWith('@android:color/');
		const key = reference.replace(/^@(android:)?color\//, '');

		value = isFramework ? (FRAMEWORK_COLORS.get(key) ?? null) : (colors.get(key) ?? null);
	}

	if (value === null || !/^#(?:[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})$/i.test(value)) return null;

	if (value.length === 9) {
		const alpha = value.slice(1, 3);
		const rgb = value.slice(3);
		return `#${rgb}${alpha}`;
	}

	return value;
}

export function resolveColorGradient(
	raw: string | null,
	colors: Map<string, string>,
	gradients: Map<string, string>
): VectorGradient | null {
	if (raw === null || !raw.startsWith('@color/')) return null;

	const source = gradients.get(raw.slice('@color/'.length));
	if (source === undefined) return null;

	return parseGradient(source, (value) => resolveColor(value, colors));
}
