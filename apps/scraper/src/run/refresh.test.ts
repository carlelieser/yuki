import { describe, expect, it } from 'vitest';
import { refreshListing, type EtagStore } from './refresh.ts';
import type { GithubClient } from '@yuki/github';
import type { GithubRelease, GithubRepository } from '@yuki/github';

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

function release(tag: string): GithubRelease {
	return {
		tag_name: tag,
		name: tag,
		body: null,
		draft: false,
		prerelease: false,
		published_at: '2026-01-01T00:00:00Z',
		assets: [
			{
				name: 'app.apk',
				browser_download_url: `https://github.com/acme/app/releases/${tag}/app.apk`,
				size: 100,
				download_count: 5,
				content_type: 'application/vnd.android.package-archive'
			}
		]
	};
}

function releaseWithoutApk(tag: string): GithubRelease {
	return {
		...release(tag),
		assets: [
			{
				name: 'sources.zip',
				browser_download_url: `https://github.com/acme/app/releases/${tag}/sources.zip`,
				size: 100,
				download_count: 5,
				content_type: 'application/zip'
			}
		]
	};
}

type ClientOverrides = Partial<{
	getRepository: GithubClient['getRepository'];
	getReadme: GithubClient['getReadme'];
	getReleases: GithubClient['getReleases'];
	getTree: GithubClient['getTree'];
	getRawFile: GithubClient['getRawFile'];
	getBlob: GithubClient['getBlob'];
}>;

function fakeClient(overrides: ClientOverrides = {}): GithubClient {
	return {
		stats: { requestCount: 0, notModifiedCount: 0 },
		getRepository: async () => ({ isModified: false }),
		getReadme: async () => ({ isModified: false }),
		getReleases: async () => ({ isModified: false }),
		getTree: async () => ({ isModified: false }),
		getRawFile: async () => ({ isModified: false }),
		getBlob: async () => ({ isModified: false }),
		...overrides
	} as unknown as GithubClient;
}

function storedEtags(entries: Record<string, string> = {}): EtagStore {
	return {
		read: async (resource) => entries[resource] ?? null,
		write: async () => {}
	};
}

const target = { owner: 'acme', name: 'app' };
const repoEtag = { 'repos/acme/app': 'main W/"repo"' };

describe('refreshListing', () => {
	it('picks up a new release when the repository itself never changed', async () => {
		const client = fakeClient({
			getReleases: async () => ({ isModified: true, body: [release('v2')], etag: 'W/"r2"' })
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.versions?.map((version) => version.tag)).toEqual(['v2']);
	});

	it('leaves screenshots untouched when the readme is unchanged', async () => {
		const client = fakeClient({
			getReleases: async () => ({ isModified: true, body: [release('v2')], etag: 'W/"r2"' })
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.screenshots).toBeNull();
	});

	it('leaves versions untouched when releases are unchanged', async () => {
		const client = fakeClient({
			getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"repo2"' })
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.versions).toBeNull();
	});

	it('reports the listing as unchanged when only a sub-resource moved', async () => {
		const client = fakeClient({
			getReleases: async () => ({ isModified: true, body: [release('v2')], etag: 'W/"r2"' })
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.listing).toBeNull();
		expect(outcome.input.owner).toBe('acme');
		expect(outcome.input.name).toBe('app');
	});

	it('skips only when every resource is unchanged', async () => {
		const outcome = await refreshListing(fakeClient(), storedEtags(repoEtag), target);

		expect(outcome.kind).toBe('skipped');
	});

	it('sends a conditional request for each sub-resource', async () => {
		const seen: (string | null)[] = [];
		const client = fakeClient({
			getReleases: async (_owner, _name, etag) => {
				seen.push(etag ?? null);
				return { isModified: false };
			},
			getReadme: async (_owner, _name, etag) => {
				seen.push(etag ?? null);
				return { isModified: false };
			},
			getTree: async (_owner, _name, _branch, etag) => {
				seen.push(etag ?? null);
				return { isModified: false };
			}
		});

		await refreshListing(
			client,
			storedEtags({
				...repoEtag,
				'repos/acme/app/releases': 'W/"rel"',
				'repos/acme/app/readme': 'W/"read"',
				'repos/acme/app/git/trees/main': 'W/"tree"'
			}),
			target
		);

		expect(seen).toEqual(['W/"rel"', 'W/"read"', 'W/"tree"']);
	});

	it('fetches the repository unconditionally so counters stay current', async () => {
		const seen: (string | null)[] = [];
		const client = fakeClient({
			getRepository: async (_owner, _name, etag) => {
				seen.push(etag ?? null);
				return { isModified: true, body: repository({ stargazers_count: 100 }), etag: 'W/"r2"' };
			}
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target);

		expect(seen).toEqual([null]);
		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.listing?.stars).toBe(100);
	});

	it('does not re-request the repository when it returns a 304', async () => {
		let repoCalls = 0;
		const client = fakeClient({
			getRepository: async () => {
				repoCalls += 1;
				return { isModified: false };
			},
			getReleases: async () => ({ isModified: true, body: [release('v2')], etag: 'W/"r2"' })
		});

		await refreshListing(client, storedEtags(repoEtag), target);

		expect(repoCalls).toBe(1);
	});
});

describe('apk requirement', () => {
	it('reports an apk when a fetched release carries one', async () => {
		const client = fakeClient({
			getReleases: async () => ({ isModified: true, body: [release('v2')], etag: 'W/"r2"' })
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.hasApk).toBe(true);
	});

	it('reports no apk when fetched releases carry none', async () => {
		const client = fakeClient({
			getReleases: async () => ({
				isModified: true,
				body: [releaseWithoutApk('v2')],
				etag: 'W/"r2"'
			})
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.hasApk).toBe(false);
	});

	it('reports no apk when the repository has no releases at all', async () => {
		const client = fakeClient({
			getReleases: async () => ({ isModified: true, body: [], etag: 'W/"r2"' })
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.hasApk).toBe(false);
	});

	it('withholds a verdict when releases were not refetched', async () => {
		const client = fakeClient({
			getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"repo2"' })
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.hasApk).toBeNull();
	});
});

describe('android structure', () => {
	function treeOf(paths: string[], truncated = false) {
		return {
			isModified: true as const,
			body: { tree: paths.map((path) => ({ path, type: 'blob' })), truncated },
			etag: 'W/"tree"'
		};
	}

	it('recognises a repository that builds an android app', async () => {
		const client = fakeClient({
			getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"repo"' }),
			getTree: async () => treeOf(['app/src/main/AndroidManifest.xml', 'app/build.gradle.kts'])
		});

		const outcome = await refreshListing(client, storedEtags(), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.isAndroidApp).toBe(true);
	});

	it('rejects a repository that only ships prose', async () => {
		const client = fakeClient({
			getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"repo"' }),
			getTree: async () => treeOf(['README.md', 'docs/privacy.html'])
		});

		const outcome = await refreshListing(client, storedEtags(), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.isAndroidApp).toBe(false);
	});

	it('withholds a verdict when the tree was not refetched', async () => {
		const client = fakeClient({
			getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"repo"' }),
			getTree: async () => ({ isModified: false })
		});

		const outcome = await refreshListing(client, storedEtags(), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.isAndroidApp).toBeNull();
	});

	it('withholds a verdict when a truncated tree hid the build files', async () => {
		const client = fakeClient({
			getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"repo"' }),
			getTree: async () => treeOf(['README.md'], true)
		});

		const outcome = await refreshListing(client, storedEtags(), target);

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.isAndroidApp).toBeNull();
	});

	it('reads the package name out of the newest release apk', async () => {
		const client = fakeClient({
			getReleases: async () => ({ isModified: true, body: [release('v2')], etag: 'W/"r2"' })
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target, {
			readApkPackage: async () => 'com.acme.app'
		});

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.packageName).toBe('com.acme.app');
	});

	it('never reads an apk for a listing that already has a package name', async () => {
		const client = fakeClient({
			getReleases: async () => ({ isModified: true, body: [release('v2')], etag: 'W/"r2"' })
		});
		const reads: string[] = [];

		const outcome = await refreshListing(
			client,
			storedEtags(repoEtag),
			{ ...target, packageName: 'com.acme.app' },
			{
				readApkPackage: async (url) => {
					reads.push(url);
					return 'com.acme.other';
				}
			}
		);

		expect(reads).toEqual([]);
		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.packageName).toBeNull();
	});

	it('leaves the package name alone when the releases did not change', async () => {
		const client = fakeClient({
			getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"repo2"' })
		});
		const reads: string[] = [];

		const outcome = await refreshListing(client, storedEtags(repoEtag), target, {
			readApkPackage: async (url) => {
				reads.push(url);
				return 'com.acme.app';
			}
		});

		expect(reads).toEqual([]);
		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.packageName).toBeNull();
	});

	it('keeps refreshing the listing when reading the apk fails', async () => {
		const client = fakeClient({
			getReleases: async () => ({ isModified: true, body: [release('v2')], etag: 'W/"r2"' })
		});

		const outcome = await refreshListing(client, storedEtags(repoEtag), target, {
			readApkPackage: async () => {
				throw new Error('range request rejected');
			}
		});

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.packageName).toBeNull();
		expect(outcome.input.versions?.map((version) => version.tag)).toEqual(['v2']);
	});

	it('skips releases that ship no apk', async () => {
		const client = fakeClient({
			getReleases: async () => ({
				isModified: true,
				body: [releaseWithoutApk('v2')],
				etag: 'W/"r2"'
			})
		});
		const reads: string[] = [];

		await refreshListing(client, storedEtags(repoEtag), target, {
			readApkPackage: async (url) => {
				reads.push(url);
				return 'com.acme.app';
			}
		});

		expect(reads).toEqual([]);
	});

	it('publishes the mapped icon and stores its bucket url', async () => {
		const client = fakeClient({
			getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"repo"' }),
			getTree: async () => treeOf(['fastlane/metadata/android/en-US/images/icon.png'])
		});
		const published: string[] = [];

		const outcome = await refreshListing(client, storedEtags(), target, {
			publishIcon: async (source) => {
				published.push(source);
				return 'https://assets.example.com/icons/abc.png';
			}
		});

		expect(published).toEqual([
			'https://raw.githubusercontent.com/acme/app/main/fastlane/metadata/android/en-US/images/icon.png'
		]);
		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.iconUrl).toBe('https://assets.example.com/icons/abc.png');
		expect(outcome.warnings).toEqual([]);
	});

	it('reports a warning and keeps the stored icon when publishing fails', async () => {
		const client = fakeClient({
			getRepository: async () => ({ isModified: true, body: repository(), etag: 'W/"repo"' }),
			getTree: async () => treeOf(['fastlane/metadata/android/en-US/images/icon.png'])
		});

		const outcome = await refreshListing(client, storedEtags(), target, {
			publishIcon: async () => {
				throw new Error('upload rejected');
			}
		});

		expect(outcome.kind).toBe('updated');
		if (outcome.kind !== 'updated') return;
		expect(outcome.input.iconUrl).toBeNull();
		expect(outcome.warnings).toEqual(['publishing the icon failed: upload rejected']);
	});
});
