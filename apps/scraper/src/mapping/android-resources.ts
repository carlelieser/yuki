const APPLICATION_TAG = /<application\b[^>]*>/;
const MANIFEST_ICON = /android:(?:roundIcon|icon)="@(drawable|mipmap)\/([A-Za-z0-9_]+)"/g;
const RESOURCE_REFERENCE = /android:(?:src|drawable)="@(drawable|mipmap)\/([A-Za-z0-9_]+)"/g;
const ADAPTIVE_RASTER_LAYER =
	/<(background|foreground)\b[^>]*android:drawable="@(android:)?(drawable|mipmap|color)\/([A-Za-z0-9_]+)"/g;

export type ResourceReference = { kind: string; name: string };

export type AdaptiveRasterLayers = {
	background: ResourceReference | null;
	foreground: ResourceReference | null;
};

export function readManifestIcon(xml: string): ResourceReference | null {
	const application = xml.match(APPLICATION_TAG)?.[0];
	if (application === undefined) return null;

	const icons = [...application.matchAll(MANIFEST_ICON)];
	const round = icons.find((match) => match[0].includes('roundIcon'));
	const chosen = icons.find((match) => !match[0].includes('roundIcon')) ?? round;
	if (chosen === undefined) return null;

	const kind = chosen[1];
	const name = chosen[2];
	if (kind === undefined || name === undefined) return null;

	return { kind, name };
}

export function readAdaptiveRasterLayers(xml: string): AdaptiveRasterLayers {
	const layers: AdaptiveRasterLayers = { background: null, foreground: null };

	for (const match of xml.matchAll(ADAPTIVE_RASTER_LAYER)) {
		const layer = match[1];
		const isFramework = match[2] !== undefined;
		const kind = match[3];
		const rawName = match[4];
		if (kind === undefined || rawName === undefined) continue;

		const name = kind === 'color' && isFramework ? `android:${rawName}` : rawName;
		if (layer === 'background') layers.background = { kind, name };
		if (layer === 'foreground') layers.foreground = { kind, name };
	}

	return layers;
}

export function readRasterReferences(xml: string): ResourceReference[] {
	const found: ResourceReference[] = [];

	for (const match of xml.matchAll(RESOURCE_REFERENCE)) {
		const kind = match[1];
		const name = match[2];
		if (kind === undefined || name === undefined) continue;
		found.push({ kind, name });
	}

	return found;
}
