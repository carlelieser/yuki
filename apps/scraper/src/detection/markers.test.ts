import { describe, expect, it } from 'vitest';
import { MARKERS, filePatternFor, markersFor } from './markers.ts';
import { buildCodeSearchQueries } from './queries.ts';

describe('markers', () => {
	it('gives every search query a marker to come from', () => {
		const literals = new Set(MARKERS.map((marker) => marker.literal));
		const queries = buildCodeSearchQueries();

		expect(
			queries.every((query) => [...literals].some((literal) => query.q.startsWith(literal)))
		).toBe(true);
	});

	it('keeps the provider the only manifest marker', () => {
		expect(markersFor('manifest').map((marker) => marker.kind)).toEqual(['provider_class']);
	});

	it('matches manifests in any module', () => {
		const pattern = filePatternFor('manifest');

		expect(pattern.test('app/src/main/AndroidManifest.xml')).toBe(true);
		expect(pattern.test('core/storage/src/main/AndroidManifest.xml')).toBe(true);
		expect(pattern.test('docs/AndroidManifest.xml.bak')).toBe(false);
	});

	it('matches every build script flavour', () => {
		const pattern = filePatternFor('build');

		expect(pattern.test('app/build.gradle')).toBe(true);
		expect(pattern.test('app/build.gradle.kts')).toBe(true);
		expect(pattern.test('gradle/libs.versions.toml')).toBe(true);
		expect(pattern.test('notes/build.gradle.md')).toBe(false);
	});

	it('gives every source marker a language so discovery can scope it', () => {
		expect(markersFor('source').every((marker) => (marker.languages ?? []).length > 0)).toBe(true);
	});
});
