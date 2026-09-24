import { describe, expect, it } from 'vitest';
import { drawableKindOf, readLayerItems } from './drawable-kind.ts';

describe('drawableKindOf', () => {
	it('identifies a vector root', () => {
		expect(drawableKindOf(`<?xml version="1.0"?><vector android:width="108dp" />`)).toBe('vector');
	});

	it('identifies a layer-list root', () => {
		expect(drawableKindOf(`<layer-list><item /></layer-list>`)).toBe('layer-list');
	});

	it('looks past a leading comment', () => {
		expect(drawableKindOf(`<!-- <vector> mentioned here --><layer-list />`)).toBe('layer-list');
	});

	it('identifies roots the renderer cannot draw', () => {
		expect(drawableKindOf(`<bitmap android:src="@mipmap/ic_launcher" />`)).toBe('bitmap');
		expect(drawableKindOf(`<selector><item /></selector>`)).toBe('selector');
	});

	it('reports unknown for anything it does not recognise', () => {
		expect(drawableKindOf(`<resources />`)).toBe('unknown');
	});
});

describe('readLayerItems', () => {
	it('reads drawable references in declaration order', () => {
		const items = readLayerItems(
			`<layer-list><item android:drawable="@mipmap/base" /><item android:drawable="@drawable/badge" /></layer-list>`
		);

		expect(items).toEqual([
			{ kind: 'reference', reference: { kind: 'mipmap', name: 'base' } },
			{ kind: 'reference', reference: { kind: 'drawable', name: 'badge' } }
		]);
	});

	it('reads an inline vector nested inside an item', () => {
		const items = readLayerItems(
			`<layer-list><item><vector android:viewportWidth="108" /></item></layer-list>`
		);

		expect(items).toHaveLength(1);
		expect(items[0]?.kind).toBe('inline');
		expect(items[0]).toMatchObject({ xml: expect.stringContaining('<vector') });
	});

	it('keeps a raster reference and an inline vector as separate layers', () => {
		const items = readLayerItems(
			`<layer-list>
				<item android:drawable="@mipmap/ic_launcher_foreground_base" />
				<item><vector android:viewportWidth="108"><path android:pathData="M0,0h1v1h-1z" /></vector></item>
			</layer-list>`
		);

		expect(items).toHaveLength(2);
		expect(items[0]).toEqual({
			kind: 'reference',
			reference: { kind: 'mipmap', name: 'ic_launcher_foreground_base' }
		});
		expect(items[1]?.kind).toBe('inline');
	});

	it('ignores comments that mention items', () => {
		const items = readLayerItems(
			`<layer-list><!-- <item android:drawable="@drawable/ghost" /> --><item android:drawable="@drawable/real" /></layer-list>`
		);

		expect(items).toEqual([{ kind: 'reference', reference: { kind: 'drawable', name: 'real' } }]);
	});
});
