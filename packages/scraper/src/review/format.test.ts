import { describe, expect, it } from 'vitest';
import { formatCandidate, formatCandidateList, parseArgs } from './format.ts';
import type { ReviewCandidate } from '../persistence/review.ts';

function candidate(overrides: Partial<ReviewCandidate> = {}): ReviewCandidate {
	return {
		slug: 'acme-app',
		title: 'Acme App',
		owner: 'acme',
		name: 'app',
		description: 'Does things',
		stars: 120,
		confidence: 'strong',
		license: 'MIT',
		repositoryUrl: 'https://github.com/acme/app',
		isArchived: false,
		isFork: false,
		iconUrl: 'https://example.com/icon.png',
		evidence: [{ kind: 'provider_class', detail: 'AndroidManifest.xml' }],
		screenshotCount: 3,
		versionCount: 12,
		...overrides
	};
}

describe('parseArgs', () => {
	it('defaults to help with no arguments', () => {
		expect(parseArgs([])).toEqual({ kind: 'help' });
	});

	it('parses pending with a limit', () => {
		expect(parseArgs(['pending', '--limit=5'])).toEqual({ kind: 'pending', limit: 5 });
	});

	it('rejects a limit that is not a positive number', () => {
		expect(parseArgs(['pending', '--limit=0']).kind).toBe('invalid');
		expect(parseArgs(['pending', '--limit=abc']).kind).toBe('invalid');
	});

	it('accepts several slugs to publish at once', () => {
		expect(parseArgs(['publish', 'one', 'two'])).toEqual({
			kind: 'publish',
			slugs: ['one', 'two']
		});
	});

	it('refuses to publish without a slug, rather than publishing everything', () => {
		expect(parseArgs(['publish']).kind).toBe('invalid');
	});

	it('rejects an unknown command', () => {
		expect(parseArgs(['delete-all']).kind).toBe('invalid');
	});
});

describe('formatCandidate', () => {
	it('shows the evidence behind the confidence rating', () => {
		const output = formatCandidate(candidate());

		expect(output).toContain('acme-app');
		expect(output).toContain('120★');
		expect(output).toContain('strong');
		expect(output).toContain('provider_class: AndroidManifest.xml');
	});

	it('flags the things a reviewer should look at twice', () => {
		const output = formatCandidate(
			candidate({ isArchived: true, isFork: true, iconUrl: null, screenshotCount: 0 })
		);

		expect(output).toContain('archived');
		expect(output).toContain('fork');
		expect(output).toContain('no icon');
		expect(output).toContain('no screenshots');
	});

	it('says so when a repo has no license instead of leaving it blank', () => {
		expect(formatCandidate(candidate({ license: null }))).toContain('no license');
	});
});

describe('formatCandidateList', () => {
	it('explains an empty result rather than printing nothing', () => {
		expect(formatCandidateList([], 'Nothing is waiting for review.')).toBe(
			'Nothing is waiting for review.'
		);
	});

	it('separates entries so they stay readable', () => {
		const output = formatCandidateList(
			[candidate({ slug: 'one' }), candidate({ slug: 'two' })],
			'empty'
		);

		expect(output).toContain('one');
		expect(output).toContain('two');
	});
});
