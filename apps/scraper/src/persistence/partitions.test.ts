import { describe, expect, it } from 'vitest';
import { isCodePartitionStale } from './partitions.ts';

const now = new Date('2026-09-12T00:00:00.000Z');

function daysAgo(days: number): Date {
	return new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
}

describe('isCodePartitionStale', () => {
	const codeKey = 'code:dev.rikka.shizuku filename:build.gradle:0..1024';

	it('keeps a recently walked code partition', () => {
		expect(isCodePartitionStale(codeKey, daysAgo(2), now)).toBe(false);
	});

	it('lets an interrupted seed resume without rewalking its own ground', () => {
		expect(isCodePartitionStale(codeKey, daysAgo(0), now)).toBe(false);
	});

	it('reopens a code partition once the window has passed', () => {
		expect(isCodePartitionStale(codeKey, daysAgo(31), now)).toBe(true);
	});

	it('never expires a repository partition, which is already date scoped', () => {
		const repoKey = 'repo:topic:shizuku:2008-01-01T00:00:00.000Z..2026-01-01T00:00:00.000Z';

		expect(isCodePartitionStale(repoKey, daysAgo(400), now)).toBe(false);
	});
});
