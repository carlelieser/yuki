import { describe, expect, it } from 'vitest';
import { MARKERS, markersFor } from './markers.ts';
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

	it('gives every source marker a language so discovery can scope it', () => {
		expect(markersFor('source').every((marker) => (marker.languages ?? []).length > 0)).toBe(true);
	});
});
