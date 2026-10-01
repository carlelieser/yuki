import { describe, expect, it } from 'vitest';
import overrides from './primary-packages.json';

describe('primary package overrides', () => {
	it('names each repository once', () => {
		const ids = overrides.map((entry) => entry.githubRepoId);
		expect(new Set(ids).size).toBe(ids.length);
	});

	it('names a plausible package for each repository', () => {
		for (const entry of overrides) {
			expect(entry.package).toMatch(/^[a-z][a-z0-9_]*(\.[a-z0-9_]+)+$/i);
			expect(entry.repository).toMatch(/^[^/]+\/[^/]+$/);
		}
	});
});
