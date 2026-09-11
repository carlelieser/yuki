const GRADIENT_TAG = /<gradient\b[^>]*>([\s\S]*?)<\/gradient>/;
const GRADIENT_ITEM = /<item\b[^>]*\/>/g;

export type GradientStop = {
	color: string;
	offset: number;
};

export type VectorGradient = {
	kind: 'linear' | 'radial';
	startX: number;
	startY: number;
	endX: number;
	endY: number;
	centerX: number;
	centerY: number;
	radius: number;
	stops: GradientStop[];
};

const MAX_STOPS = 16;

function attribute(source: string, name: string): string | null {
	return source.match(new RegExp(`android:${name}="([^"]*)"`))?.[1] ?? null;
}

function numeric(source: string, name: string, fallback: number): number {
	const raw = attribute(source, name);
	if (raw === null) return fallback;

	const parsed = Number.parseFloat(raw);
	return Number.isFinite(parsed) ? parsed : fallback;
}

function readStops(body: string, toColor: (raw: string | null) => string | null): GradientStop[] {
	const stops: GradientStop[] = [];

	for (const match of body.matchAll(GRADIENT_ITEM)) {
		if (stops.length >= MAX_STOPS) break;

		const color = toColor(attribute(match[0], 'color'));
		if (color === null) continue;

		stops.push({ color, offset: numeric(match[0], 'offset', stops.length === 0 ? 0 : 1) });
	}

	return stops;
}

export function parseGradient(
	source: string,
	toColor: (raw: string | null) => string | null
): VectorGradient | null {
	const match = source.match(GRADIENT_TAG);
	if (match === null) return null;

	const tag = match[0];
	const stops = readStops(match[1] ?? '', toColor);
	if (stops.length === 0) return null;

	const kind = attribute(tag, 'type') === 'radial' ? 'radial' : 'linear';

	return {
		kind,
		startX: numeric(tag, 'startX', 0),
		startY: numeric(tag, 'startY', 0),
		endX: numeric(tag, 'endX', 0),
		endY: numeric(tag, 'endY', 0),
		centerX: numeric(tag, 'centerX', 0),
		centerY: numeric(tag, 'centerY', 0),
		radius: numeric(tag, 'gradientRadius', 0),
		stops
	};
}

export function gradientToSvg(gradient: VectorGradient, id: string): string {
	const stops = gradient.stops
		.map((stop) => `<stop offset="${stop.offset}" stop-color="${stop.color}"/>`)
		.join('');

	if (gradient.kind === 'radial') {
		if (gradient.radius <= 0) return '';

		return (
			`<radialGradient id="${id}" gradientUnits="userSpaceOnUse"` +
			` cx="${gradient.centerX}" cy="${gradient.centerY}" r="${gradient.radius}">` +
			`${stops}</radialGradient>`
		);
	}

	return (
		`<linearGradient id="${id}" gradientUnits="userSpaceOnUse"` +
		` x1="${gradient.startX}" y1="${gradient.startY}"` +
		` x2="${gradient.endX}" y2="${gradient.endY}">` +
		`${stops}</linearGradient>`
	);
}
