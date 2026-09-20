import { gradientToSvg } from './vector-gradient.ts';
import {
	resolveColor,
	resolveColorGradient,
	vectorToSvg,
	CANVAS,
	VIEWPORT_INSET,
	CORNER_RADIUS
} from './vector-icon.ts';

function normaliseLayer(svg: string): string {
	const inner = svg.replace(/^<svg[^>]*>/, '').replace(/<\/svg>$/, '');
	const viewBox = svg.match(/viewBox="([^"]+)"/)?.[1];
	if (viewBox === undefined) return inner;

	const [, , rawWidth, rawHeight] = viewBox.split(/\s+/).map((part) => Number.parseFloat(part));
	const width = rawWidth !== undefined && rawWidth > 0 ? rawWidth : CANVAS;
	const height = rawHeight !== undefined && rawHeight > 0 ? rawHeight : CANVAS;

	const scaleX = CANVAS / width;
	const scaleY = CANVAS / height;
	if (scaleX === 1 && scaleY === 1) return inner;

	return `<g transform="scale(${scaleX} ${scaleY})">${inner}</g>`;
}

export function composeAdaptiveSvg(input: {
	background: { kind: 'color' | 'vector'; value: string } | null;
	foreground: string | null;
	colors: Map<string, string>;
	gradients?: Map<string, string>;
}): string | null {
	const gradients = input.gradients ?? new Map<string, string>();
	const foreground =
		input.foreground === null
			? null
			: vectorToSvg(input.foreground, input.colors, { idPrefix: 'fg', gradients });
	if (foreground === null) return null;

	const width = CANVAS;
	const height = CANVAS;
	const inner = normaliseLayer(foreground);

	const layers: string[] = [];

	if (input.background !== null) {
		if (input.background.kind === 'color') {
			const color = resolveColor(input.background.value, input.colors);
			const gradient = resolveColorGradient(input.background.value, input.colors, gradients);

			if (gradient !== null) {
				const definition = gradientToSvg(gradient, 'bgc');
				if (definition === '') {
					const first = gradient.stops[0]?.color;
					if (first !== undefined)
						layers.push(`<rect width="${width}" height="${height}" fill="${first}"/>`);
				} else {
					layers.push(
						`<defs>${definition}</defs><rect width="${width}" height="${height}" fill="url(#bgc)"/>`
					);
				}
			} else if (color !== null) {
				layers.push(`<rect width="${width}" height="${height}" fill="${color}"/>`);
			}
		} else {
			const backgroundSvg = vectorToSvg(input.background.value, input.colors, {
				idPrefix: 'bg',
				gradients
			});
			if (backgroundSvg !== null) layers.push(normaliseLayer(backgroundSvg));
		}
	}

	layers.push(inner);

	const inset = (VIEWPORT_INSET / CANVAS) * Math.min(width, height);
	const visibleWidth = width - inset * 2;
	const visibleHeight = height - inset * 2;
	const radius = CORNER_RADIUS * Math.min(visibleWidth, visibleHeight);
	const clip = `<clipPath id="c"><rect x="${inset}" y="${inset}" width="${visibleWidth}" height="${visibleHeight}" rx="${radius}"/></clipPath>`;

	return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${inset} ${inset} ${visibleWidth} ${visibleHeight}">${clip}<g clip-path="url(#c)">${layers.join('')}</g></svg>`;
}

export function toDataUri(svg: string): string {
	return `data:image/svg+xml;base64,${Buffer.from(svg, 'utf8').toString('base64')}`;
}
