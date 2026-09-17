import { describe, expect, it } from 'vitest';
import { hasAndroidStructure, mergeEvidence, scoreConfidence } from './evidence.ts';

describe('scoreConfidence', () => {
	it('rates the provider class as strong', () => {
		expect(scoreConfidence([{ kind: 'provider_class', detail: null }])).toBe('strong');
	});

	it('rates a runtime api call as strong', () => {
		expect(scoreConfidence([{ kind: 'runtime_api_call', detail: null }])).toBe('strong');
	});

	it('rates the current gradle coordinate as probable because linking is not using', () => {
		expect(scoreConfidence([{ kind: 'gradle_dependency', detail: null }])).toBe('probable');
	});

	it('leaves a root app that only borrows a shizuku utility short of strong', () => {
		expect(
			scoreConfidence([
				{ kind: 'gradle_dependency', detail: 'dev.rikka.shizuku in build.gradle' },
				{ kind: 'source_filename', detail: 'PixelLauncherModsRootService.kt' }
			])
		).toBe('probable');
	});

	it('rates an app that registers the provider and calls the api as strong', () => {
		expect(
			scoreConfidence([
				{ kind: 'provider_class', detail: 'AndroidManifest.xml' },
				{ kind: 'runtime_api_call', detail: 'Shizuku.pingBinder in Kotlin source' },
				{ kind: 'gradle_dependency', detail: 'dev.rikka.shizuku in build.gradle.kts' }
			])
		).toBe('strong');
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

	it('rates a readme mention as weak', () => {
		expect(scoreConfidence([{ kind: 'readme_mention', detail: null }])).toBe('weak');
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

describe('hasAndroidStructure', () => {
	it('accepts a manifest', () => {
		expect(hasAndroidStructure(['app/src/main/AndroidManifest.xml'])).toBe(true);
	});

	it('accepts a groovy build script', () => {
		expect(hasAndroidStructure(['app/build.gradle'])).toBe(true);
	});

	it('accepts a kotlin build script', () => {
		expect(hasAndroidStructure(['app/build.gradle.kts'])).toBe(true);
	});

	it('accepts a version catalog', () => {
		expect(hasAndroidStructure(['gradle/libs.versions.toml'])).toBe(true);
	});

	it('rejects a repository that only ships prose', () => {
		expect(hasAndroidStructure(['README.md', 'docs/index.html', 'LICENSE'])).toBe(false);
	});

	it('rejects an empty tree', () => {
		expect(hasAndroidStructure([])).toBe(false);
	});

	it('does not mistake a similarly named file for a build script', () => {
		expect(hasAndroidStructure(['notes/build.gradle.md', 'AndroidManifest.xml.bak'])).toBe(false);
	});
});
