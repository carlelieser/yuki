import { describe, expect, it } from 'vitest';
import {
	RESULT_CAP,
	buildCodeSearchQueries,
	isMarkdownPath,
	isSliceTruncated,
	isSourceFilenameMatch
} from './queries.ts';

describe('buildCodeSearchQueries', () => {
	it('slices the two queries that exceed the result cap', () => {
		const providerQueries = buildCodeSearchQueries().filter((query) =>
			query.q.startsWith('rikka.shizuku.ShizukuProvider')
		);

		expect(providerQueries.length).toBeGreaterThan(1);
		expect(providerQueries.every((query) => query.q.includes('size:'))).toBe(true);
	});

	it('drops the redundant permission marker', () => {
		const queries = buildCodeSearchQueries();

		expect(queries.some((query) => query.q.includes('API_V23'))).toBe(false);
	});
});

describe('isMarkdownPath', () => {
	it('rejects the wikis and awesome-lists that merely mention Shizuku', () => {
		expect(isMarkdownPath('README.md')).toBe(true);
		expect(isMarkdownPath('docs/guide.MDX')).toBe(true);
	});

	it('accepts build and manifest files', () => {
		expect(isMarkdownPath('app/build.gradle.kts')).toBe(false);
		expect(isMarkdownPath('app/src/main/AndroidManifest.xml')).toBe(false);
	});
});

describe('isSourceFilenameMatch', () => {
	it('matches Shizuku-named Kotlin and Java sources', () => {
		expect(isSourceFilenameMatch('app/src/ShizukuService.kt')).toBe(true);
		expect(isSourceFilenameMatch('app/src/ShizukuHelper.java')).toBe(true);
	});

	it('ignores non-source files and unrelated names', () => {
		expect(isSourceFilenameMatch('docs/shizuku.md')).toBe(false);
		expect(isSourceFilenameMatch('app/src/MainActivity.kt')).toBe(false);
	});
});

describe('isSliceTruncated', () => {
	it('flags a slice that still exceeds what pagination can reach', () => {
		expect(isSliceTruncated(RESULT_CAP + 1)).toBe(true);
		expect(isSliceTruncated(RESULT_CAP)).toBe(false);
	});
});
