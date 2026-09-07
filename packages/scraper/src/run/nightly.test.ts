import { describe, expect, it } from 'vitest';
import { runNightly, type RunPorts } from './nightly.ts';
import type { GithubClient } from '../github/client.ts';
import type { GithubRepository } from '../github/types.ts';
import type { ListingRecord, PersistInput } from '../persistence/listings.ts';

function repository(overrides: Partial<GithubRepository> = {}): GithubRepository {
	return {
		id: 1,
		name: 'app',
		full_name: 'acme/app',
		description: null,
		html_url: 'https://github.com/acme/app',
		homepage: null,
		default_branch: 'main',
		stargazers_count: 0,
		fork: false,
		archived: false,
		pushed_at: null,
		license: null,
		owner: { login: 'acme', html_url: 'https://github.com/acme' },
		...overrides
	};
}

type ClientOverrides = Partial<{
	getRepository: GithubClient['getRepository'];
	getReadme: GithubClient['getReadme'];
	getReleases: GithubClient['getReleases'];
	getTree: GithubClient['getTree'];
}>;

function fakeClient(overrides: ClientOverrides = {}): GithubClient {
	return {
		stats: { requestCount: 3, notModifiedCount: 1 },
		getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"a"' }),
		getReadme: async () => ({ isModified: false }),
		getReleases: async () => ({ isModified: true, body: [], etag: null }),
		getTree: async () => ({ isModified: false }),
		...overrides
	} as unknown as GithubClient;
}

function ports(overrides: Partial<RunPorts> = {}): RunPorts & { persisted: PersistInput[] } {
	const persisted: PersistInput[] = [];

	return {
		client: fakeClient(),
		etags: { read: async () => null, write: async () => {} },
		listTargets: async () => [],
		persist: async (input) => {
			persisted.push(input);
			return 'listing-id';
		},
		touch: async () => {},
		persisted,
		...overrides
	} as RunPorts & { persisted: PersistInput[] };
}

const options = { shouldDiscover: false, maxRepos: 10, maxRefresh: 10 };

const knownListing: ListingRecord = {
	id: 'listing-1',
	owner: 'acme',
	name: 'app',
	githubRepoId: 1
};

describe('runNightly', () => {
	it('counts a 304 as skipped rather than updated', async () => {
		const runPorts = ports({
			client: fakeClient({ getRepository: async () => ({ isModified: false }) }),
			listTargets: async () => [knownListing]
		});

		const summary = await runNightly(runPorts, options);

		expect(summary.updatedCount).toBe(0);
		expect(summary.skippedCount).toBe(1);
		expect(runPorts.persisted).toHaveLength(0);
	});

	it('advances lastScrapedAt on a 304 so the cursor keeps moving', async () => {
		const touched: string[] = [];
		const runPorts = ports({
			client: fakeClient({ getRepository: async () => ({ isModified: false }) }),
			listTargets: async () => [knownListing],
			touch: async (listingId) => {
				touched.push(listingId);
			}
		});

		await runNightly(runPorts, options);

		expect(touched).toEqual(['listing-1']);
	});

	it('persists a listing that changed', async () => {
		const runPorts = ports({ listTargets: async () => [knownListing] });

		const summary = await runNightly(runPorts, options);

		expect(summary.updatedCount).toBe(1);
		expect(runPorts.persisted[0]?.listing.slug).toBe('acme-app');
	});

	it('keeps going when one listing fails and records why', async () => {
		let call = 0;
		const runPorts = ports({
			client: fakeClient({
				getRepository: async () => {
					call += 1;
					if (call === 1) throw new Error('boom');
					return { isModified: true, body: repository({ id: 2, name: 'other' }), etag: null };
				}
			}),
			listTargets: async () => [
				knownListing,
				{ id: 'listing-2', owner: 'acme', name: 'other', githubRepoId: 2 }
			]
		});

		const summary = await runNightly(runPorts, options);

		expect(summary.skippedCount).toBe(1);
		expect(summary.updatedCount).toBe(1);
		expect(summary.warnings).toContain('acme/app: boom');
	});

	it('reports the client request counters for the run', async () => {
		const summary = await runNightly(ports({ listTargets: async () => [knownListing] }), options);

		expect(summary.requestCount).toBe(3);
		expect(summary.notModifiedCount).toBe(1);
	});

	it('propagates a failure from listing the targets so the run closes as failed', async () => {
		const runPorts = ports({
			listTargets: async () => {
				throw new Error('database is down');
			}
		});

		await expect(runNightly(runPorts, options)).rejects.toThrow('database is down');
	});

	it('skips the refresh fetch for a repo discovery just returned', async () => {
		let repoCalls = 0;
		const runPorts = ports({
			client: fakeClient({
				getRepository: async () => {
					repoCalls += 1;
					return { isModified: true, body: repository(), etag: null };
				}
			}),
			listTargets: async () => [knownListing]
		});

		const summary = await runNightly(runPorts, options);

		expect(repoCalls).toBe(1);
		expect(summary.updatedCount).toBe(1);
	});
});
