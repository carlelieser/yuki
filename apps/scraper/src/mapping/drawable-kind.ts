const ROOT_TAG =
	/<(vector|layer-list|adaptive-icon|bitmap|selector|ripple|shape|inset|animated-vector)\b/;

const LAYER_ITEM = /<item\b([^>]*?)(\/?)>/g;
const ITEM_CLOSE = '</item>';

export type DrawableKind =
	| 'vector'
	| 'layer-list'
	| 'adaptive-icon'
	| 'bitmap'
	| 'selector'
	| 'ripple'
	| 'shape'
	| 'inset'
	| 'animated-vector'
	| 'unknown';

export type LayerItem =
	| { kind: 'reference'; reference: { kind: string; name: string } }
	| { kind: 'inline'; xml: string };

export function drawableKindOf(xml: string): DrawableKind {
	const stripped = xml.replace(/<\?[^?]*\?>/g, '').replace(/<!--[\s\S]*?-->/g, '');
	const tag = stripped.match(ROOT_TAG)?.[1];

	return tag === undefined ? 'unknown' : (tag as DrawableKind);
}

export function readLayerItems(xml: string): LayerItem[] {
	const stripped = xml.replace(/<!--[\s\S]*?-->/g, '');
	const items: LayerItem[] = [];

	LAYER_ITEM.lastIndex = 0;

	for (let match = LAYER_ITEM.exec(stripped); match !== null; match = LAYER_ITEM.exec(stripped)) {
		const attributes = match[1] ?? '';
		const isSelfClosing = match[2] === '/';

		const reference = attributes.match(
			/android:drawable="@(android:)?(drawable|mipmap|color)\/([A-Za-z0-9_]+)"/
		);

		if (reference !== null) {
			const kind = reference[2];
			const rawName = reference[3];
			if (kind !== undefined && rawName !== undefined) {
				const name =
					kind === 'color' && reference[1] !== undefined ? `android:${rawName}` : rawName;
				items.push({ kind: 'reference', reference: { kind, name } });
			}

			if (!isSelfClosing) LAYER_ITEM.lastIndex = skipItem(stripped, LAYER_ITEM.lastIndex);
			continue;
		}

		if (isSelfClosing) continue;

		const end = stripped.indexOf(ITEM_CLOSE, LAYER_ITEM.lastIndex);
		const body =
			end === -1 ? stripped.slice(LAYER_ITEM.lastIndex) : stripped.slice(LAYER_ITEM.lastIndex, end);
		LAYER_ITEM.lastIndex = end === -1 ? stripped.length : end + ITEM_CLOSE.length;

		if (body.trim() !== '') items.push({ kind: 'inline', xml: body });
	}

	return items;
}

function skipItem(source: string, from: number): number {
	const end = source.indexOf(ITEM_CLOSE, from);
	return end === -1 ? from : end + ITEM_CLOSE.length;
}
