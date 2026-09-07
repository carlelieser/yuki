import { describe, expect, it } from 'vitest';
import { mergeEvidence, scoreConfidence } from './evidence.ts';

describe('scoreConfidence', () => {
	it('rates the provider class as strong', () => {
		expect(scoreConfidence([{ kind: 'provider_class', detail: null }])).toBe('strong');
	});

	it('rates the current gradle coordinate as strong', () => {
		expect(scoreConfidence([{ kind: 'gradle_dependency', detail: null }])).toBe('strong');
	});

	it('rates the legacy coordinate as probable', () => {
		expect(scoreConfidence([{ kind: 'legacy_gradle_dependency', detail: null }])).toBe('probable');
	});

	it('rates a source filename alone as probable', () => {
		expect(scoreConfidence([{ kind: 'source_filename', detail: null }])).toBe('probable');
	});

	it('rates a bare topic as weak', () => {
		expect(scoreConfidence([{ kind: 'repository_topic', detail: null }])).toBe('weak');
	});

	it('takes the strongest signal present', () => {
		expect(
			scoreConfidence([
				{ kind: 'repository_topic', detail: null },
				{ kind: 'provider_class', detail: null }
			])
		).toBe('strong');
	});

	it('rates no evidence as weak', () => {
		expect(scoreConfidence([])).toBe('weak');
	});
});

describe('mergeEvidence', () => {
	it('keeps one row per kind so the unique constraint holds', () => {
		const merged = mergeEvidence([
			{ kind: 'gradle_dependency', detail: 'build.gradle' },
			{ kind: 'gradle_dependency', detail: 'build.gradle.kts' },
			{ kind: 'provider_class', detail: 'AndroidManifest.xml' }
		]);

		expect(merged).toEqual([
			{ kind: 'gradle_dependency', detail: 'build.gradle' },
			{ kind: 'provider_class', detail: 'AndroidManifest.xml' }
		]);
	});
});
