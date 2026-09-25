import type { StoredObject } from './r2-bucket.ts';

export const ICON_PREFIX = 'icons/';

export type PruneWindow = {
	now: Date;
	graceMillis: number;
};

export function iconKeyFromUrl(url: string, assetsBaseUrl: string): string | null {
	const prefix = `${assetsBaseUrl.replace(/\/+$/, '')}/`;
	if (!url.startsWith(prefix)) return null;

	const key = url.slice(prefix.length);
	return key.startsWith(ICON_PREFIX) ? key : null;
}

export function findOrphanedIcons(
	objects: StoredObject[],
	referencedKeys: ReadonlySet<string>,
	window: PruneWindow
): StoredObject[] {
	const cutoff = window.now.getTime() - window.graceMillis;

	return objects.filter(
		(object) =>
			object.key.startsWith(ICON_PREFIX) &&
			!referencedKeys.has(object.key) &&
			object.lastModified.getTime() < cutoff
	);
}
