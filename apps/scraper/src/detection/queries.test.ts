import { describe, expect, it } from 'vitest';
import {
	GITHUB_EPOCH,
	RESULT_CAP,
	buildCodeSearchQueries,
	isIndivisible,
	isMarkdownPath,
	isSliceTruncated,
	isSourceFilenameMatch,
	splitRange,
	splitSizeRange,
	withCreatedRange,
	withSizeRange,
	isSizeIndivisible,
	FULL_SIZE_RANGE,
	type SizeRange
} from './queries.ts';

describe('buildCodeSearchQueries', () => {
	it('leaves size partitioning to the walker', () => {
		expect(buildCodeSearchQueries().every((query) => !query.q.includes('size:'))).toBe(true);
	});

	it('emits each query exactly once', () => {
		const queries = buildCodeSearchQueries().map((query) => query.q);
		expect(new Set(queries).size).toBe(queries.length);
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

describe('date ranges', () => {
	const day = (value: string) => new Date(`${value}T00:00:00.000Z`);

	it('formats a created range as a GitHub qualifier', () => {
		expect(
			withCreatedRange('topic:shizuku', { since: day('2020-01-01'), until: day('2020-06-30') })
		).toBe('topic:shizuku created:2020-01-01..2020-06-30');
	});

	it('splits a range into halves that do not overlap', () => {
		const [left, right] = splitRange({ since: day('2020-01-01'), until: day('2020-01-11') });

		expect(left.since).toEqual(day('2020-01-01'));
		expect(left.until).toEqual(day('2020-01-06'));
		expect(right.since).toEqual(day('2020-01-07'));
		expect(right.until).toEqual(day('2020-01-11'));
	});

	it('treats a single day as indivisible', () => {
		expect(isIndivisible({ since: day('2020-01-01'), until: day('2020-01-02') })).toBe(true);
		expect(isIndivisible({ since: day('2020-01-01'), until: day('2020-01-05') })).toBe(false);
	});

	it('always terminates when splitting repeatedly', () => {
		let range = { since: GITHUB_EPOCH, until: day('2026-01-01') };
		let depth = 0;

		while (!isIndivisible(range)) {
			range = splitRange(range)[0];
			depth += 1;
			if (depth > 100) break;
		}

		expect(depth).toBeLessThan(20);
	});
});

describe('size ranges', () => {
	it('formats bounded and unbounded ranges', () => {
		expect(withSizeRange('q', { from: 0, to: 100 })).toBe('q size:0..100');
		expect(withSizeRange('q', { from: 500, to: null })).toBe('q size:>=500');
	});

	it('splits a bounded range into halves that do not overlap', () => {
		const [left, right] = splitSizeRange({ from: 0, to: 100 });
		expect(left).toEqual({ from: 0, to: 50 });
		expect(right).toEqual({ from: 51, to: 100 });
	});

	it('always makes progress on an unbounded range', () => {
		let range: SizeRange = FULL_SIZE_RANGE;

		for (let i = 0; i < 12; i += 1) {
			const [, upper] = splitSizeRange(range);
			expect(upper.from).toBeGreaterThan(range.from);
			range = upper;
		}
	});

	it('terminates when splitting a bounded range repeatedly', () => {
		let range: SizeRange = { from: 0, to: 100000 };
		let depth = 0;

		while (!isSizeIndivisible(range)) {
			range = splitSizeRange(range)[0];
			depth += 1;
			if (depth > 200) break;
		}

		expect(depth).toBeLessThan(30);
	});
});
