import type { GithubClient, GithubTree } from '@yuki/github';
import { GithubSkip } from '@yuki/github';

export async function readOptional<Body>(
	load: () => Promise<{ isModified: true; body: Body; etag: string | null } | { isModified: false }>
): Promise<Body | null> {
	try {
		const response = await load();
		return response.isModified ? response.body : null;
	} catch (cause) {
		if (cause instanceof GithubSkip) return null;
		throw cause;
	}
}

import { composeAdaptiveRaster, toPngDataUri } from '../mapping/adaptive-raster.ts';
import { findExpoConfigPaths, readExpoIcon, resolveExpoAsset } from '../mapping/expo-config.ts';
import { buildVectorIcon, readColorResources, resourceDirFor } from '../mapping/adaptive-vector.ts';
import { readSolidFill, solidLayer } from '../mapping/solid-layer.ts';
import { parseAdaptiveIcon, resolveColor } from '../mapping/vector-icon.ts';
import { readLayerItems } from '../mapping/drawable-kind.ts';
import {
	buildBlobUrl,
	buildIconUrl,
	findAdaptiveIconPath,
	findCuratedIconPath,
	findDeclaredIconPaths,
	findManifestPaths,
	findPrebakedIconPath,
	findRasterForReference,
	findVectorForReference,
	pickBestDeclared,
	readAdaptiveRasterLayers,
	readManifestIcon,
	readRasterReferences,
	type ResourceReference
} from '../mapping/icon.ts';

export async function iconFrom(
	client: GithubClient,
	tree: GithubTree,
	owner: string,
	name: string,
	branch: string
): Promise<string | null> {
	if (tree.truncated) return null;

	const read = async (path: string): Promise<string | null> => {
		const response = await readOptional(() => client.getRawFile(owner, name, path, branch));
		return response ?? null;
	};

	const readBlob = async (sha: string): Promise<string | null> => {
		const response = await readOptional(() => client.getBlob(owner, name, sha));
		return response ?? null;
	};

	const curatedUrl = await buildIconUrl(tree, owner, name, branch, readBlob, findCuratedIconPath);
	if (curatedUrl !== null) return curatedUrl;

	const rasterFallback = async (declaredXml: string | null): Promise<string | null> => {
		const composed = await composedRasterFrom(tree, read, owner, name, branch, declaredXml);
		if (composed !== null) return composed;

		const discovered = await discoveredRasterFrom(tree, read, owner, name, branch);
		if (discovered !== null) return discovered;

		return buildIconUrl(tree, owner, name, branch, readBlob, findPrebakedIconPath);
	};

	const declared = await declaredIconFrom(tree, read);
	if (declared !== null) {
		if (declared.xml !== null) {
			const vector = await buildVectorIcon(tree, read, {
				declaredPath: declared.xml,
				download: (path) => downloadBlob(owner, name, branch, path)
			});
			if (vector !== null && !vector.fidelity.unresolved) return vector.svg;

			if (vector !== null) {
				const raster = await rasterFallback(declared.xml);
				if (raster !== null) return raster;
				return vector.svg;
			}
		}

		if (declared.layers !== null) {
			const colors =
				declared.xml === null
					? new Map<string, string>()
					: await readColorResources(tree, read, resourceDirFor(declared.xml));

			const composed = await composeLayers(
				declared.layers,
				(path) => downloadBlob(owner, name, branch, path),
				read,
				colors
			);
			if (composed !== null) return composed;

			return buildBlobUrl(owner, name, branch, declared.layers.foreground, false);
		}

		if (declared.raster !== null) {
			return buildBlobUrl(owner, name, branch, declared.raster, false);
		}
	}

	const vector = await buildVectorIcon(tree, read, {
		download: (path) => downloadBlob(owner, name, branch, path)
	});
	if (vector !== null && !vector.fidelity.unresolved) return vector.svg;

	const discovered = await discoveredRasterFrom(tree, read, owner, name, branch);
	if (discovered !== null) return discovered;

	const prebakedUrl = await buildIconUrl(tree, owner, name, branch, readBlob, findPrebakedIconPath);
	if (prebakedUrl !== null) return prebakedUrl;

	if (vector !== null) return vector.svg;

	return expoIconFrom(tree, read, owner, name, branch);
}

async function expoIconFrom(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	owner: string,
	name: string,
	branch: string
): Promise<string | null> {
	for (const configPath of findExpoConfigPaths(tree)) {
		const config = await read(configPath);
		if (config === null) continue;

		const declared = readExpoIcon(config);
		if (declared === null) continue;

		const asset = resolveExpoAsset(tree, configPath, declared.foreground);
		if (asset === null) continue;

		return buildBlobUrl(owner, name, branch, asset, false);
	}

	return null;
}

async function composedRasterFrom(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	owner: string,
	name: string,
	branch: string,
	declaredXml: string | null
): Promise<string | null> {
	if (declaredXml === null) return null;

	const adaptiveXml = await read(declaredXml);
	if (adaptiveXml === null) return null;

	const refs = parseAdaptiveIcon(adaptiveXml);
	if (refs.foreground === null) return null;

	const foreground = await rasterForLayer(tree, read, refs.foreground);
	if (foreground === null) return null;

	const background =
		refs.background === null ? null : await rasterForLayer(tree, read, refs.background);

	const colors = await readColorResources(tree, read, resourceDirFor(declaredXml));

	const composed = await composeLayers(
		{
			foreground,
			background: background === null ? null : { kind: 'raster', path: background }
		},
		(path) => downloadBlob(owner, name, branch, path),
		read,
		colors
	);
	if (composed !== null) return composed;

	return buildBlobUrl(owner, name, branch, foreground, false);
}

async function rasterForLayer(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	reference: { kind: string; name: string }
): Promise<string | null> {
	if (reference.kind === 'color') return null;

	const direct = findRasterForReference(tree, { kind: reference.kind, name: reference.name });
	if (direct !== null) return direct;

	const wrappers = findDeclaredIconPaths(tree, {
		kind: reference.kind,
		name: reference.name
	}).xml;

	for (const wrapper of wrappers) {
		const xml = await read(wrapper);
		if (xml === null) continue;

		for (const nested of readLayerItems(xml)) {
			if (nested.kind !== 'reference' || nested.reference.kind === 'color') continue;

			const raster = findRasterForReference(tree, nested.reference);
			if (raster !== null) return raster;
		}

		for (const nested of readRasterReferences(xml)) {
			const raster = findRasterForReference(tree, nested);
			if (raster !== null) return raster;
		}
	}

	return null;
}

async function discoveredRasterFrom(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	owner: string,
	name: string,
	branch: string
): Promise<string | null> {
	const adaptivePath = findAdaptiveIconPath(tree);
	if (adaptivePath === null) return null;

	const adaptiveXml = await read(adaptivePath);
	if (adaptiveXml === null) return null;

	const foreground = readAdaptiveRasterLayers(adaptiveXml).foreground;
	if (foreground === null) return null;

	const wrappers = findDeclaredIconPaths(tree, foreground).xml;
	const resolved = await resolveIconXml(tree, read, wrappers);
	if (resolved.raster === null) return null;

	return buildBlobUrl(owner, name, branch, resolved.raster, false);
}

async function downloadBlob(
	owner: string,
	name: string,
	branch: string,
	path: string
): Promise<Buffer | null> {
	const response = await fetch(buildBlobUrl(owner, name, branch, path, false));
	if (!response.ok) return null;

	return Buffer.from(await response.arrayBuffer());
}

async function composeLayers(
	layers: AdaptiveLayers,
	download: (path: string) => Promise<Buffer | null>,
	read: (path: string) => Promise<string | null>,
	colors: Map<string, string>
): Promise<string | null> {
	const foreground = await download(layers.foreground);
	if (foreground === null) return null;

	const background = await backgroundLayer(layers.background, download, read, colors);
	const composed = await composeAdaptiveRaster(background ?? Buffer.alloc(0), foreground);

	return composed === null ? null : toPngDataUri(composed);
}

async function backgroundLayer(
	background: AdaptiveBackground | null,
	download: (path: string) => Promise<Buffer | null>,
	read: (path: string) => Promise<string | null>,
	colors: Map<string, string>
): Promise<Buffer | null> {
	if (background === null) return null;

	if (background.kind === 'raster') return download(background.path);

	if (background.kind === 'color') {
		const name = background.reference.name;
		const reference = name.startsWith('android:')
			? `@android:color/${name.slice('android:'.length)}`
			: `@color/${name}`;

		const colour = resolveColor(reference, colors);
		return colour === null ? null : solidLayer(colour);
	}

	const xml = await read(background.path);
	if (xml === null) return null;

	const fill = readSolidFill(xml, colors);
	return fill === null ? null : solidLayer(fill);
}

type AdaptiveBackground =
	| { kind: 'raster'; path: string }
	| { kind: 'vector'; path: string }
	| { kind: 'color'; reference: ResourceReference };

type AdaptiveLayers = {
	background: AdaptiveBackground | null;
	foreground: string;
};

type DeclaredIcon = {
	xml: string | null;
	raster: string | null;
	layers: AdaptiveLayers | null;
};

async function declaredIconFrom(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>
): Promise<DeclaredIcon | null> {
	for (const manifestPath of findManifestPaths(tree)) {
		const manifest = await read(manifestPath);
		if (manifest === null) continue;

		const icon = readManifestIcon(manifest);
		if (icon === null) continue;

		const found = findDeclaredIconPaths(tree, icon);
		const resolved = await resolveIconXml(tree, read, found.xml);
		if (resolved.layers !== null || resolved.raster !== null) return resolved;

		const direct = pickBestDeclared(found.raster);
		if (direct !== null) return { xml: found.xml[0] ?? null, raster: direct, layers: null };

		if (resolved.xml !== null) return resolved;
	}

	return null;
}

export async function resolveIconXml(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>,
	candidates: string[]
): Promise<DeclaredIcon> {
	for (const path of candidates) {
		const xml = await read(path);
		if (xml === null) continue;

		const layers = rasterLayersIn(tree, xml);
		if (layers !== null) return { xml: candidates[0] ?? null, raster: null, layers };

		for (const reference of readRasterReferences(xml)) {
			const raster = findRasterForReference(tree, reference);
			if (raster !== null) return { xml: candidates[0] ?? null, raster, layers: null };
		}
	}

	return { xml: candidates[0] ?? null, raster: null, layers: null };
}

function rasterLayersIn(tree: GithubTree, xml: string): AdaptiveLayers | null {
	const layers = readAdaptiveRasterLayers(xml);
	if (layers.foreground === null) return null;

	const foreground = findRasterForReference(tree, layers.foreground);
	if (foreground === null) return null;

	if (layers.background === null) return { background: null, foreground };

	const raster = findRasterForReference(tree, layers.background);
	if (raster !== null) return { background: { kind: 'raster', path: raster }, foreground };

	const reference = layers.background;
	if (reference.kind === 'color') {
		return { background: { kind: 'color', reference }, foreground };
	}

	const vector = findVectorForReference(tree, reference);
	if (vector !== null) return { background: { kind: 'vector', path: vector }, foreground };

	return { background: null, foreground };
}
