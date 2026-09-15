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
import { buildVectorIcon } from '../mapping/adaptive-vector.ts';
import {
	buildBlobUrl,
	buildIconUrl,
	findAdaptiveIconPath,
	findDeclaredIconPaths,
	findManifestPath,
	findRasterForReference,
	pickBestDeclared,
	readAdaptiveRasterLayers,
	readManifestIcon,
	readRasterReferences
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

	const rasterUrl = await buildIconUrl(tree, owner, name, branch, readBlob);
	if (rasterUrl !== null) return rasterUrl;

	const declared = await declaredIconFrom(tree, read);
	if (declared !== null) {
		if (declared.xml !== null) {
			const svg = await buildVectorIcon(tree, read, declared.xml);
			if (svg !== null) return svg;
		}

		if (declared.layers !== null) {
			const composed = await composeLayers(declared.layers, (path) =>
				downloadBlob(owner, name, branch, path)
			);
			if (composed !== null) return composed;

			return buildBlobUrl(owner, name, branch, declared.layers.foreground, false);
		}

		if (declared.raster !== null) {
			return buildBlobUrl(owner, name, branch, declared.raster, false);
		}
	}

	const vector = await buildVectorIcon(tree, read);
	if (vector !== null) return vector;

	return discoveredRasterFrom(tree, read, owner, name, branch);
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
	layers: { background: string | null; foreground: string },
	download: (path: string) => Promise<Buffer | null>
): Promise<string | null> {
	const foreground = await download(layers.foreground);
	if (foreground === null) return null;

	const background = layers.background === null ? null : await download(layers.background);
	const composed = composeAdaptiveRaster(background ?? Buffer.alloc(0), foreground);

	return composed === null ? null : toPngDataUri(composed);
}

type DeclaredIcon = {
	xml: string | null;
	raster: string | null;
	layers: { background: string | null; foreground: string } | null;
};

async function declaredIconFrom(
	tree: GithubTree,
	read: (path: string) => Promise<string | null>
): Promise<DeclaredIcon | null> {
	const manifestPath = findManifestPath(tree);
	if (manifestPath === null) return null;

	const manifest = await read(manifestPath);
	if (manifest === null) return null;

	const icon = readManifestIcon(manifest);
	if (icon === null) return null;

	const found = findDeclaredIconPaths(tree, icon);
	const direct = pickBestDeclared(found.raster);
	if (direct !== null) return { xml: found.xml[0] ?? null, raster: direct, layers: null };

	return resolveIconXml(tree, read, found.xml);
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

function rasterLayersIn(
	tree: GithubTree,
	xml: string
): { background: string | null; foreground: string } | null {
	const layers = readAdaptiveRasterLayers(xml);
	if (layers.foreground === null) return null;

	const foreground = findRasterForReference(tree, layers.foreground);
	if (foreground === null) return null;

	const background =
		layers.background === null ? null : findRasterForReference(tree, layers.background);

	return { background, foreground };
}
