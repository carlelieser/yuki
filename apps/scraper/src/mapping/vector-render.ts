import {
	gradientToSvg,
	parseGradient,
	readAaptAttr,
	type VectorGradient
} from './vector-gradient.ts';
import { parseVectorTree, type VectorGroup, type VectorPath } from './vector-tree.ts';
import { resolveColor, resolveColorGradient } from './vector-colors.ts';
import { attribute, escapeXml, numeric } from './vector-attributes.ts';

const MAX_PATHS = 64;

function transformOf(group: string): string[] {
	const parts: string[] = [];
	const translateX = numeric(group, 'translateX', 0);
	const translateY = numeric(group, 'translateY', 0);
	const scaleX = numeric(group, 'scaleX', 1);
	const scaleY = numeric(group, 'scaleY', 1);
	const rotation = numeric(group, 'rotation', 0);
	const pivotX = numeric(group, 'pivotX', 0);
	const pivotY = numeric(group, 'pivotY', 0);

	if (translateX !== 0 || translateY !== 0) parts.push(`translate(${translateX} ${translateY})`);

	const pivoted = pivotX !== 0 || pivotY !== 0;
	if (pivoted) parts.push(`translate(${pivotX} ${pivotY})`);
	if (rotation !== 0) parts.push(`rotate(${rotation})`);
	if (scaleX !== 1 || scaleY !== 1) parts.push(`scale(${scaleX} ${scaleY})`);
	if (pivoted) parts.push(`translate(${-pivotX} ${-pivotY})`);

	return parts;
}

type RenderContext = {
	colors: Map<string, string>;
	fallbackFill: string | null;
	idPrefix: string;
	gradients: Map<string, string>;
	definitions: string[];
	clips: string[];
	budget: { paths: number };
};

function paintOf(
	gradient: VectorGradient | null,
	solid: string | null,
	whenMissing: string | null,
	context: RenderContext
): string | null {
	if (gradient !== null) {
		const id = `${context.idPrefix}${context.definitions.length}`;
		const definition = gradientToSvg(gradient, id);
		if (definition !== '') {
			context.definitions.push(definition);
			return `url(#${id})`;
		}

		return gradient.stops[0]?.color ?? solid ?? whenMissing;
	}

	return solid ?? whenMissing;
}

function strokeAttributes(tag: string, paint: string, width: number): string[] {
	const attributes = [`stroke="${paint}"`, `stroke-width="${width}"`];

	const cap = attribute(tag, 'strokeLineCap');
	const join = attribute(tag, 'strokeLineJoin');
	const alpha = attribute(tag, 'strokeAlpha');
	if (cap !== null) attributes.push(`stroke-linecap="${escapeXml(cap)}"`);
	if (join !== null) attributes.push(`stroke-linejoin="${escapeXml(join)}"`);
	if (alpha !== null) attributes.push(`stroke-opacity="${escapeXml(alpha)}"`);

	return attributes;
}

function renderPath(path: VectorPath, context: RenderContext): string | null {
	const { tag, body } = path;
	const data = attribute(tag, 'pathData');
	if (data === null || data.trim() === '') return null;

	const { colors, gradients } = context;
	const toColor = (raw: string | null) => resolveColor(raw, colors);

	const fillGradient =
		parseGradient(readAaptAttr(body, 'fillColor') ?? '', toColor) ??
		resolveColorGradient(attribute(tag, 'fillColor'), colors, gradients);
	const strokeGradient =
		parseGradient(readAaptAttr(body, 'strokeColor') ?? '', toColor) ??
		resolveColorGradient(attribute(tag, 'strokeColor'), colors, gradients);

	const fill = resolveColor(attribute(tag, 'fillColor'), colors);
	const stroke = resolveColor(attribute(tag, 'strokeColor'), colors);
	const strokeWidth = numeric(tag, 'strokeWidth', 0);

	const hasStroke = (stroke !== null || strokeGradient !== null) && strokeWidth > 0;
	const declaresFill = attribute(tag, 'fillColor') !== null || fillGradient !== null;
	const declaresStroke = attribute(tag, 'strokeColor') !== null || strokeGradient !== null;
	if (!declaresFill && !declaresStroke) return null;

	const attributes = [`d="${escapeXml(data.trim())}"`];

	const fillPaint = paintOf(fillGradient, fill, hasStroke ? 'none' : context.fallbackFill, context);
	if (fillPaint === null) return null;
	attributes.push(`fill="${fillPaint}"`);

	if (hasStroke) {
		const strokePaint = paintOf(strokeGradient, stroke, null, context);
		if (strokePaint !== null) attributes.push(...strokeAttributes(tag, strokePaint, strokeWidth));
	}

	const alpha = attribute(tag, 'fillAlpha');
	if (alpha !== null) attributes.push(`fill-opacity="${escapeXml(alpha)}"`);

	if (attribute(tag, 'fillType')?.toLowerCase() === 'evenodd') {
		attributes.push('fill-rule="evenodd"');
	}

	return `<path ${attributes.join(' ')}/>`;
}

function clipAttribute(clips: string[], context: RenderContext): string {
	if (clips.length === 0) return '';

	const id = `${context.idPrefix}clip${context.clips.length}`;
	const paths = clips.map((data) => `<path d="${escapeXml(data.trim())}"/>`).join('');
	context.clips.push(`<clipPath id="${id}">${paths}</clipPath>`);

	return ` clip-path="url(#${id})"`;
}

function renderImage(tag: string): string | null {
	const href = attribute(tag, 'href');
	if (href === null || !href.startsWith('data:image/')) return null;

	const width = numeric(tag, 'width', 0);
	const height = numeric(tag, 'height', 0);
	if (width <= 0 || height <= 0) return null;

	return `<image x="0" y="0" width="${width}" height="${height}" preserveAspectRatio="none" xlink:href="${escapeXml(href)}"/>`;
}

function renderGroup(group: VectorGroup, context: RenderContext): string {
	const rendered: string[] = [];

	for (const child of group.children) {
		if (context.budget.paths <= 0) break;

		if (child.kind === 'image') {
			const image = renderImage(child.tag);
			if (image === null) continue;

			context.budget.paths -= 1;
			rendered.push(image);
			continue;
		}

		if (child.kind === 'path') {
			const path = renderPath(child.path, context);
			if (path === null) continue;

			context.budget.paths -= 1;
			rendered.push(path);
			continue;
		}

		const inner = renderGroup(child.group, context);
		if (inner !== '') rendered.push(inner);
	}

	if (rendered.length === 0) return '';

	const body = rendered.join('');
	const clip = clipAttribute(group.clips, context);
	const transform = group.tag === '' ? null : transformOf(group.tag).join(' ') || null;
	if (transform === null && clip === '') return body;

	const attributes = transform === null ? '' : ` transform="${transform}"`;
	return `<g${attributes}${clip}>${body}</g>`;
}

export function convertPaths(
	vector: string,
	colors: Map<string, string>,
	fallbackFill: string | null,
	idPrefix: string,
	gradients: Map<string, string>
): string {
	const tree = parseVectorTree(vector);
	if (tree === null) return '';

	const context: RenderContext = {
		colors,
		fallbackFill,
		idPrefix,
		gradients,
		definitions: [],
		clips: [],
		budget: { paths: MAX_PATHS }
	};

	const body = renderGroup(tree, context);
	if (body === '') return '';

	const defs = [...context.definitions, ...context.clips];
	return defs.length === 0 ? body : `<defs>${defs.join('')}</defs>${body}`;
}
