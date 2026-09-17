import type { EvidenceKind } from '@yuki/db/schema';

export type MarkerScope = 'manifest' | 'build' | 'source';

export type Marker = {
	kind: EvidenceKind;
	scope: MarkerScope;
	literal: string;
	languages?: string[];
};

export const MANIFEST_FILE = /(^|\/)AndroidManifest\.xml$/;
export const BUILD_FILE = /(^|\/)(build\.gradle(\.kts)?|libs\.versions\.toml)$/;
export const SOURCE_FILE = /\.(kt|java)$/;

export const SCOPE_FILENAMES: Record<MarkerScope, string[]> = {
	manifest: ['AndroidManifest.xml'],
	build: ['build.gradle', 'build.gradle.kts', 'libs.versions.toml'],
	source: []
};

export const MARKERS: Marker[] = [
	{ kind: 'provider_class', scope: 'manifest', literal: 'rikka.shizuku.ShizukuProvider' },
	{ kind: 'gradle_dependency', scope: 'build', literal: 'dev.rikka.shizuku' },
	{ kind: 'legacy_gradle_dependency', scope: 'build', literal: 'moe.shizuku.api' },
	{
		kind: 'runtime_api_call',
		scope: 'source',
		literal: 'Shizuku.pingBinder',
		languages: ['kotlin', 'java']
	},
	{
		kind: 'runtime_api_call',
		scope: 'source',
		literal: 'Shizuku.checkSelfPermission',
		languages: ['kotlin', 'java']
	},
	{
		kind: 'runtime_api_call',
		scope: 'source',
		literal: 'Shizuku.requestPermission',
		languages: ['kotlin', 'java']
	},
	{
		kind: 'runtime_api_call',
		scope: 'source',
		literal: 'Shizuku.newProcess',
		languages: ['kotlin']
	},
	{
		kind: 'runtime_api_call',
		scope: 'source',
		literal: 'Shizuku.addRequestPermissionResultListener',
		languages: ['kotlin']
	}
];

export function markersFor(scope: MarkerScope): Marker[] {
	return MARKERS.filter((marker) => marker.scope === scope);
}

export function filePatternFor(scope: MarkerScope): RegExp {
	if (scope === 'manifest') return MANIFEST_FILE;
	if (scope === 'build') return BUILD_FILE;
	return SOURCE_FILE;
}

export function describeMarker(marker: Marker, path: string): string {
	return `${marker.literal} in ${path}`;
}
