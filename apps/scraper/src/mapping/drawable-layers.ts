import { drawableKindOf, readLayerItems } from './drawable-kind.ts';
import { markUnresolved, type Fidelity } from './fidelity.ts';

const MAX_LAYER_DEPTH = 4;
const RASTER_CANVAS = 108;
const VECTOR_OPEN = /<vector\b[^>]*>/;
const INSET_OPEN = /<inset\b([^>]*?)(\/?)>/;
const REFERENCE = /@(?:android:)?(drawable|mipmap|color)\/([A-Za-z0-9_]+)/;
const DIMENSION = /^(-?[\d.]+)(%|dp|dip|px)?$/;

export type DrawableReference = { kind: string; name: string };

export type DrawableSources = {
	readDrawable: (reference: DrawableReference) => Promise<string | null>;
	readRaster: ((reference: DrawableReference) => Promise<string | null>) | null;
	fidelity: Fidelity;
};

type Insets = { left: number; top: number; right: number; bottom: number };

type Viewport = { width: number; height: number };

export async function flattenReference(
	reference: DrawableReference,
	sources: DrawableSources,
	depth = 0
): Promise<string | null> {
	if (reference.kind !== 'drawable' && reference.kind !== 'mipmap') {
		markUnresolved(sources.fidelity, `layer-reference:${reference.kind}`);
		return null;
	}

	const xml = await sources.readDrawable(reference);
	if (xml !== null) return flattenDrawable(xml, sources, depth);
	if (reference.kind === 'drawable' && sources.readRaster === null) {
		markUnresolved(sources.fidelity, `missing-drawable:${reference.name}`);
		return null;
	}

	return rasterLayer(reference, sources, `layer-reference:${reference.kind}`);
}

export async function rasterLayer(
	reference: DrawableReference,
	sources: DrawableSources,
	unresolvedReason: string
): Promise<string | null> {
	if (sources.readRaster === null) {
		markUnresolved(sources.fidelity, unresolvedReason);
		return null;
	}

	const uri = await sources.readRaster(reference);
	if (uri === null) {
		markUnresolved(sources.fidelity, `missing-raster:${reference.name}`);
		return null;
	}

	return `<vector android:viewportWidth="${RASTER_CANVAS}" android:viewportHeight="${RASTER_CANVAS}"><image android:width="${RASTER_CANVAS}" android:height="${RASTER_CANVAS}" android:href="${uri}" /></vector>`;
}

export async function flattenDrawable(
	xml: string,
	sources: DrawableSources,
	depth = 0
): Promise<string | null> {
	const kind = drawableKindOf(xml);
	if (kind === 'vector') return xml;

	if (depth >= MAX_LAYER_DEPTH) {
		markUnresolved(sources.fidelity, 'layer-list-too-deep');
		return null;
	}

	if (kind === 'layer-list') return flattenLayerList(xml, sources, depth);
	if (kind === 'inset') return flattenInset(xml, sources, depth);
	if (kind === 'bitmap') return flattenBitmap(xml, sources);

	markUnresolved(sources.fidelity, `unsupported-drawable:${kind}`);
	return null;
}

async function flattenLayerList(
	xml: string,
	sources: DrawableSources,
	depth: number
): Promise<string | null> {
	const items = readLayerItems(xml);
	if (items.length === 0) {
		markUnresolved(sources.fidelity, 'layer-list-empty');
		return null;
	}

	const rendered: string[] = [];
	for (const item of items) {
		const flat =
			item.kind === 'reference'
				? await flattenReference(item.reference, sources, depth + 1)
				: await flattenDrawable(item.xml, sources, depth + 1);
		if (flat !== null) rendered.push(flat);
	}

	if (rendered.length === 0) return null;

	return rendered.length === 1 ? (rendered[0] ?? null) : mergeVectors(rendered);
}

async function flattenBitmap(xml: string, sources: DrawableSources): Promise<string | null> {
	const source = xml.match(/android:src="([^"]+)"/)?.[1];
	const reference = source?.match(REFERENCE);
	const kind = reference?.[1];
	const name = reference?.[2];

	if (kind === undefined || name === undefined) {
		markUnresolved(sources.fidelity, 'bitmap-without-source');
		return null;
	}

	return rasterLayer({ kind, name }, sources, `layer-reference:${kind}`);
}

async function flattenInset(
	xml: string,
	sources: DrawableSources,
	depth: number
): Promise<string | null> {
	const open = xml.match(INSET_OPEN);
	if (open === null) return null;

	const attributes = open[1] ?? '';
	const child = await insetChild(xml, open, sources, depth);
	if (child === null) return null;

	const viewport = viewportOf(child);
	if (viewport === null) return null;

	return insetVector(child, readInsets(attributes, intrinsicWidth(child)), viewport);
}

async function insetChild(
	xml: string,
	open: RegExpMatchArray,
	sources: DrawableSources,
	depth: number
): Promise<string | null> {
	const drawable = (open[1] ?? '').match(/android:drawable="([^"]+)"/)?.[1];
	const reference = drawable?.match(REFERENCE);
	const kind = reference?.[1];
	const name = reference?.[2];
	if (kind !== undefined && name !== undefined) {
		return flattenReference({ kind, name }, sources, depth + 1);
	}

	const start = (open.index ?? 0) + open[0].length;
	const end = xml.lastIndexOf('</inset>');
	const body = end === -1 ? '' : xml.slice(start, end).trim();
	if (body === '') {
		markUnresolved(sources.fidelity, 'inset-without-drawable');
		return null;
	}

	return flattenDrawable(body, sources, depth + 1);
}

function readInsets(attributes: string, intrinsic: number | null): Insets {
	const read = (name: string): string | null =>
		attributes.match(new RegExp(`android:${name}="([^"]+)"`))?.[1] ?? null;
	const all = read('inset');
	const raw = {
		left: read('insetLeft') ?? all,
		top: read('insetTop') ?? all,
		right: read('insetRight') ?? all,
		bottom: read('insetBottom') ?? all
	};

	const horizontal = [raw.left, raw.right];
	const vertical = [raw.top, raw.bottom];
	return {
		left: fractionOf(raw.left, intrinsic, horizontal),
		top: fractionOf(raw.top, intrinsic, vertical),
		right: fractionOf(raw.right, intrinsic, horizontal),
		bottom: fractionOf(raw.bottom, intrinsic, vertical)
	};
}

function fractionOf(
	value: string | null,
	intrinsic: number | null,
	axis: (string | null)[]
): number {
	const parsed = parseDimension(value);
	if (parsed === null) return 0;
	if (parsed.isFraction) return parsed.amount;

	const absolute = axis.map(parseDimension).filter((entry) => entry !== null && !entry.isFraction);
	const total =
		(intrinsic ?? RASTER_CANVAS) + absolute.reduce((sum, entry) => sum + (entry?.amount ?? 0), 0);
	return total > 0 ? parsed.amount / total : 0;
}

function parseDimension(value: string | null): { amount: number; isFraction: boolean } | null {
	const match = value?.trim().match(DIMENSION);
	if (match === null || match === undefined) return null;

	const amount = Number.parseFloat(match[1] ?? '');
	if (!Number.isFinite(amount)) return null;

	return match[2] === '%'
		? { amount: amount / 100, isFraction: true }
		: { amount, isFraction: false };
}

function intrinsicWidth(vector: string): number | null {
	const header = vector.match(VECTOR_OPEN)?.[0] ?? '';
	const width = Number.parseFloat(header.match(/android:width="([\d.]+)/)?.[1] ?? '');
	return Number.isFinite(width) && width > 0 ? width : null;
}

function insetVector(child: string, insets: Insets, viewport: Viewport): string | null {
	const scaleX = round(1 - insets.left - insets.right);
	const scaleY = round(1 - insets.top - insets.bottom);
	if (scaleX <= 0 || scaleY <= 0) return null;

	const translateX = round(insets.left * viewport.width);
	const translateY = round(insets.top * viewport.height);

	return `<vector android:viewportWidth="${viewport.width}" android:viewportHeight="${viewport.height}"><group android:translateX="${translateX}" android:translateY="${translateY}" android:scaleX="${scaleX}" android:scaleY="${scaleY}">${bodyOf(child)}</group></vector>`;
}

function round(value: number): number {
	return Number(value.toFixed(4));
}

function viewportOf(vector: string): Viewport | null {
	const header = vector.match(VECTOR_OPEN)?.[0];
	if (header === undefined) return null;

	const width = Number.parseFloat(header.match(/android:viewportWidth="([\d.]+)"/)?.[1] ?? '');
	const height = Number.parseFloat(header.match(/android:viewportHeight="([\d.]+)"/)?.[1] ?? '');
	if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) return null;

	return { width, height };
}

function bodyOf(vector: string): string {
	const header = vector.match(VECTOR_OPEN)?.[0];
	if (header === undefined) return '';

	return vector.slice(vector.indexOf(header) + header.length).replace(/<\/vector>\s*$/, '');
}

function mergeVectors(layers: string[]): string | null {
	const base = layers[0];
	const target = base === undefined ? null : viewportOf(base);
	if (target === null) return null;

	const bodies: string[] = [];
	for (const layer of layers) {
		const inner = bodyOf(layer);
		const viewport = viewportOf(layer);
		if (inner.trim() === '' || viewport === null) continue;

		const scaleX = target.width / viewport.width;
		const scaleY = target.height / viewport.height;
		bodies.push(
			scaleX === 1 && scaleY === 1
				? inner
				: `<group android:scaleX="${scaleX}" android:scaleY="${scaleY}">${inner}</group>`
		);
	}

	if (bodies.length === 0) return null;

	return `<vector xmlns:android="http://schemas.android.com/apk/res/android" android:viewportWidth="${target.width}" android:viewportHeight="${target.height}">${bodies.join('')}</vector>`;
}
