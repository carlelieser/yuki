import { describe, expect, it } from 'vitest';
import { collectRepositoryEvidence } from './reverify.ts';
import type { GithubClient } from '@yuki/github';

function fakeClient(files: Record<string, string>): GithubClient {
	const paths = Object.keys(files);

	return {
		stats: { requestCount: 0, notModifiedCount: 0 },
		getTree: async () => ({
			isModified: true,
			body: {
				truncated: false,
				tree: paths.map((path) => ({ path, type: 'blob', sha: path }))
			}
		}),
		getBlob: async (_owner: string, _name: string, sha: string) => ({
			isModified: true,
			body: files[sha] ?? ''
		})
	} as unknown as GithubClient;
}

describe('collectRepositoryEvidence', () => {
	it('recovers the provider evidence a truncated discovery band missed', async () => {
		const client = fakeClient({
			'app/src/main/AndroidManifest.xml':
				'<provider android:name="rikka.shizuku.ShizukuProvider" />',
			'gradle/libs.versions.toml': 'shizuku = { module = "dev.rikka.shizuku:api" }'
		});

		const evidence = await collectRepositoryEvidence(client, 'mihonapp', 'mihon', 'main');

		expect(evidence.map((entry) => entry.kind).sort()).toEqual([
			'gradle_dependency',
			'provider_class'
		]);
	});

	it('rates a root app that only borrows a utility as gradle evidence alone', async () => {
		const client = fakeClient({
			'app/build.gradle': 'implementation "dev.rikka.shizuku:api:13.1.5"',
			'app/src/main/AndroidManifest.xml': '<application />',
			'app/src/main/java/PixelLauncherModsRootService.kt':
				'import rikka.shizuku.SystemServiceHelper'
		});

		const evidence = await collectRepositoryEvidence(
			client,
			'KieronQuinn',
			'PixelLauncherMods',
			'main'
		);

		expect(evidence.map((entry) => entry.kind)).toEqual(['gradle_dependency']);
	});

	it('records a runtime call as its own evidence kind', async () => {
		const client = fakeClient({
			'app/src/main/java/ShizukuInstaller.kt': 'if (!Shizuku.pingBinder()) return'
		});

		const evidence = await collectRepositoryEvidence(client, 'acme', 'app', 'main');

		expect(evidence.map((entry) => entry.kind).sort()).toEqual([
			'runtime_api_call',
			'source_filename'
		]);
	});

	it('finds a runtime call in a Sui-named file that no filename heuristic would match', async () => {
		const client = fakeClient({
			'app/build.gradle': 'implementation "dev.rikka.shizuku:api:13.1.5"',
			'app/src/main/java/provider/root/SuiFileServiceLauncher.kt':
				'if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {'
		});

		const evidence = await collectRepositoryEvidence(client, 'zhanghai', 'MaterialFiles', 'main');

		expect(evidence.map((entry) => entry.kind).sort()).toEqual([
			'gradle_dependency',
			'runtime_api_call'
		]);
	});

	it('reads the legacy coordinate as its own kind', async () => {
		const client = fakeClient({ 'build.gradle': 'implementation "moe.shizuku.api:provider:1.0"' });

		const evidence = await collectRepositoryEvidence(client, 'acme', 'legacy', 'main');

		expect(evidence.map((entry) => entry.kind)).toEqual(['legacy_gradle_dependency']);
	});

	it('finds no evidence in a repository that merely mentions Shizuku in prose', async () => {
		const client = fakeClient({ 'README.md': 'Works great with Shizuku!' });

		const evidence = await collectRepositoryEvidence(client, 'acme', 'docs', 'main');

		expect(evidence).toEqual([]);
	});

	it('keeps one row per kind so the unique constraint holds', async () => {
		const client = fakeClient({
			'app/build.gradle': 'dev.rikka.shizuku:api',
			'core/build.gradle': 'dev.rikka.shizuku:provider'
		});

		const evidence = await collectRepositoryEvidence(client, 'acme', 'app', 'main');
		const kinds = evidence.map((entry) => entry.kind);

		expect(new Set(kinds).size).toBe(kinds.length);
	});
});
