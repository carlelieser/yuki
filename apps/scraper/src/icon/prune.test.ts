import { describe, expect, it } from 'vitest';
import { findOrphanedIcons, iconKeyFromUrl } from './prune.ts';

const now = new Date('2026-09-25T12:00:00Z');
const window = { now, graceMillis: 24 * 60 * 60 * 1000 };
const old = new Date('2026-09-20T00:00:00Z');

describe('iconKeyFromUrl', () => {
	it('recovers the icon key from a url on the assets origin', () => {
		expect(
			iconKeyFromUrl('https://assets.example.com/icons/a.png', 'https://assets.example.com/')
		).toBe('icons/a.png');
	});

	it('ignores urls hosted elsewhere or outside the icon prefix', () => {
		expect(
			iconKeyFromUrl('https://raw.githubusercontent.com/a/b/icon.png', 'https://assets.example.com')
		).toBeNull();
		expect(
			iconKeyFromUrl('https://assets.example.com/avatars/u/a.png', 'https://assets.example.com')
		).toBeNull();
	});
});

describe('findOrphanedIcons', () => {
	it('returns stored icons that no listing references', () => {
		const orphans = findOrphanedIcons(
			[
				{ key: 'icons/kept.png', lastModified: old },
				{ key: 'icons/orphan.svg', lastModified: old }
			],
			new Set(['icons/kept.png']),
			window
		);

		expect(orphans.map((object) => object.key)).toEqual(['icons/orphan.svg']);
	});

	it('spares icons uploaded within the grace period so an in-flight scrape can reference them', () => {
		const orphans = findOrphanedIcons(
			[{ key: 'icons/fresh.png', lastModified: new Date('2026-09-25T06:00:00Z') }],
			new Set(),
			window
		);

		expect(orphans).toEqual([]);
	});

	it('never touches objects outside the icon prefix', () => {
		const orphans = findOrphanedIcons(
			[{ key: 'avatars/u/a.png', lastModified: old }],
			new Set(),
			window
		);

		expect(orphans).toEqual([]);
	});
});
