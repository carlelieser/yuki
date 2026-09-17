import { describe, expect, it } from 'vitest';
import { collectRepositoryEvidence } from './reverify.ts';
import { scoreConfidence } from './evidence.ts';
import { isCatalogueExcluded, shouldPublish } from '../persistence/listings.ts';
import type { GithubClient } from '@yuki/github';

function fakeClient(matches: (query: string) => string[], seen: string[] = []): GithubClient {
	return {
		stats: { requestCount: 0, notModifiedCount: 0, pacedWaitMs: 0 },
		searchCode: async (q: string) => {
			seen.push(q);
			const paths = matches(q);
			return {
				isModified: true,
				body: { total_count: paths.length, items: paths.map((path) => ({ path })) }
			};
		}
	} as unknown as GithubClient;
}

async function verify(client: GithubClient, owner = 'acme', name = 'app') {
	const verdict = await collectRepositoryEvidence(client, owner, name);
	if (verdict.kind !== 'verified') throw new Error(`expected verified, got ${verdict.kind}`);
	return verdict.evidence;
}

describe('collectRepositoryEvidence', () => {
	it('scopes every query to the repository under review', async () => {
		const seen: string[] = [];
		await collectRepositoryEvidence(
			fakeClient(() => [], seen),
			'mihonapp',
			'mihon'
		);

		expect(seen.every((query) => query.includes('repo:mihonapp/mihon'))).toBe(true);
	});

	it.each([
		['vvb2060/KeyAttestation', 'Shizuku.pingBinder', 'app/src/main/java/.../home/HomeFragment.kt'],
		[
			'RikkaApps/WADB',
			'Shizuku.checkSelfPermission',
			'app/src/main/java/.../events/GlobalRequestHandler.java'
		],
		[
			'andreknieriem/open-headunit',
			'Shizuku.pingBinder',
			'app/src/main/java/.../utils/SUExecutor.kt'
		],
		['Turbo1123/TurboIMS', 'Shizuku.pingBinder', 'app/src/main/java/.../ims/MainActivity.java'],
		[
			'zhanghai/MaterialFiles',
			'Shizuku.checkSelfPermission',
			'app/src/main/java/.../root/SuiFileServiceLauncher.kt'
		]
	])('finds the runtime call %s keeps in an unhinted filename', async (_repo, marker, path) => {
		const evidence = await verify(fakeClient((query) => (query.startsWith(marker) ? [path] : [])));

		expect(evidence.map((entry) => entry.kind)).toContain('runtime_api_call');
		expect(scoreConfidence(evidence)).toBe('strong');
	});

	it('leaves a repository that only links the library short of strong', async () => {
		const evidence = await verify(
			fakeClient((query) => (query.startsWith('dev.rikka.shizuku') ? ['app/build.gradle'] : []))
		);

		expect(evidence.map((entry) => entry.kind)).toEqual(['gradle_dependency']);
		expect(scoreConfidence(evidence)).toBe('probable');
	});

	it('ignores prose that merely mentions Shizuku', async () => {
		expect(await verify(fakeClient(() => ['README.md']))).toEqual([]);
	});

	it('reports inconclusive when nothing matched, since an unindexed repo looks the same', async () => {
		const verdict = await collectRepositoryEvidence(
			fakeClient(() => []),
			'acme',
			'app'
		);

		expect(verdict.kind).toBe('inconclusive');
	});

	it('stops querying once the evidence is already strong', async () => {
		const seen: string[] = [];
		await verify(
			fakeClient(
				(query) =>
					query.startsWith('rikka.shizuku.ShizukuProvider') ? ['AndroidManifest.xml'] : [],
				seen
			)
		);

		expect(seen).toHaveLength(1);
	});

	it('reports inconclusive rather than empty when a query cannot be read', async () => {
		const client = {
			stats: { requestCount: 0, notModifiedCount: 0, pacedWaitMs: 0 },
			searchCode: async () => ({ isModified: false })
		} as unknown as GithubClient;

		const verdict = await collectRepositoryEvidence(client, 'acme', 'app');

		expect(verdict.kind).toBe('inconclusive');
	});
});

describe('catalogue exclusions', () => {
	it('keeps the Shizuku platform itself out of a catalogue of apps that use it', () => {
		expect(isCatalogueExcluded('RikkaApps', 'Shizuku')).toBe(true);
		expect(isCatalogueExcluded('RikkaApps', 'Sui')).toBe(true);
	});

	it('does not exclude other apps by the same author', () => {
		expect(isCatalogueExcluded('RikkaApps', 'WADB')).toBe(false);
	});

	it('never publishes an excluded repository however strong its evidence', () => {
		expect(
			shouldPublish({
				owner: 'RikkaApps',
				name: 'Shizuku',
				confidence: 'strong',
				hasDownloadableAsset: true
			})
		).toBe(false);
	});

	it('publishes on strong evidence with a downloadable asset', () => {
		expect(
			shouldPublish({
				owner: 'acme',
				name: 'app',
				confidence: 'strong',
				hasDownloadableAsset: true
			})
		).toBe(true);
	});

	it('withholds publication without an asset or without strong evidence', () => {
		expect(
			shouldPublish({
				owner: 'acme',
				name: 'app',
				confidence: 'strong',
				hasDownloadableAsset: false
			})
		).toBe(false);
		expect(
			shouldPublish({
				owner: 'acme',
				name: 'app',
				confidence: 'probable',
				hasDownloadableAsset: true
			})
		).toBe(false);
	});
});
