import { describe, expect, it } from 'vitest';
import {
	findGradleFiles,
	findManifestFiles,
	parseGradlePackage,
	parseManifestPackage,
	resolvePackageIdentity
} from './package-name.ts';
import type { GithubTree } from '../github/types.ts';

function tree(paths: string[], truncated = false): GithubTree {
	return { tree: paths.map((path) => ({ path, type: 'blob' })), truncated };
}

describe('parseGradlePackage', () => {
	it('reads a Groovy applicationId', () => {
		const parsed = parseGradlePackage(`android {
			defaultConfig {
				applicationId "dev.yuki.sample"
				versionCode 12
				versionName "1.2.3"
			}
		}`);

		expect(parsed).toEqual({
			packageName: 'dev.yuki.sample',
			versionName: '1.2.3',
			versionCode: '12'
		});
	});

	it('reads a Kotlin DSL applicationId', () => {
		const parsed = parseGradlePackage(`android {
			defaultConfig {
				applicationId = "dev.yuki.sample"
				versionCode = 34
				versionName = "2.0.0"
			}
		}`);

		expect(parsed).toEqual({
			packageName: 'dev.yuki.sample',
			versionName: '2.0.0',
			versionCode: '34'
		});
	});

	it('accepts single quotes', () => {
		expect(parseGradlePackage(`applicationId 'dev.yuki.sample'`).packageName).toBe(
			'dev.yuki.sample'
		);
	});

	it('prefers applicationId over namespace', () => {
		const parsed = parseGradlePackage(`android {
			namespace = "dev.yuki.internal"
			defaultConfig { applicationId = "dev.yuki.public" }
		}`);

		expect(parsed.packageName).toBe('dev.yuki.public');
	});

	it('uses namespace only in an application module', () => {
		const application = parseGradlePackage(`plugins { id "com.android.application" }
			android { namespace = "dev.yuki.app" }`);

		expect(application.packageName).toBe('dev.yuki.app');
	});

	it('recognises an application declared through a version catalog alias', () => {
		const parsed = parseGradlePackage(`plugins {
				alias(libs.plugins.agp.app)
			}
			android { namespace = "me.weishu.kernelsu" }`);

		expect(parsed.packageName).toBe('me.weishu.kernelsu');
	});

	it('does not treat a catalog library alias as an application', () => {
		const parsed = parseGradlePackage(`plugins {
				alias(libs.plugins.agp.lib)
			}
			android { namespace = "dev.yuki.library" }`);

		expect(parsed.packageName).toBeNull();
	});

	it('prefers a literal applicationId over the namespace', () => {
		const parsed = parseGradlePackage(`plugins { alias(libs.plugins.android.application) }
			android {
				namespace = "dev.yuki.internal"
				defaultConfig { applicationId = "dev.yuki.store" }
			}`);

		expect(parsed.packageName).toBe('dev.yuki.store');
	});

	it('ignores the namespace of a library module', () => {
		const library = parseGradlePackage(`plugins { id "com.android.library" }
			android { namespace = "dev.yuki.library" }`);

		expect(library.packageName).toBeNull();
	});

	it('rejects an interpolated applicationId rather than guessing', () => {
		const parsed = parseGradlePackage(`applicationId "\${rootProject.appId}"`);

		expect(parsed.packageName).toBeNull();
	});

	it('rejects a version name built from a variable', () => {
		const parsed = parseGradlePackage(`versionName "\${versions.app}"`);

		expect(parsed.versionName).toBeNull();
	});

	it('rejects a single-segment package', () => {
		expect(parseGradlePackage(`applicationId "sample"`).packageName).toBeNull();
	});

	it('ignores commented-out declarations', () => {
		const parsed = parseGradlePackage(`android {
			// applicationId "dev.yuki.commented"
			/* applicationId "dev.yuki.blockcomment" */
			defaultConfig { applicationId "dev.yuki.real" }
		}`);

		expect(parsed.packageName).toBe('dev.yuki.real');
	});

	it('returns nulls for a file with no identity', () => {
		expect(parseGradlePackage(`dependencies { implementation "androidx.core:core" }`)).toEqual({
			packageName: null,
			versionName: null,
			versionCode: null
		});
	});
});

describe('parseManifestPackage', () => {
	it('reads the package attribute', () => {
		const source = `<?xml version="1.0"?>
			<manifest xmlns:android="http://schemas.android.com/apk/res/android"
				package="dev.yuki.sample">
			</manifest>`;

		expect(parseManifestPackage(source)).toBe('dev.yuki.sample');
	});

	it('ignores a package attribute on another element', () => {
		const source = `<manifest xmlns:android="x"><uses-sdk package="dev.yuki.other" /></manifest>`;

		expect(parseManifestPackage(source)).toBeNull();
	});

	it('returns null when the manifest carries no package', () => {
		expect(parseManifestPackage(`<manifest xmlns:android="x"></manifest>`)).toBeNull();
	});
});

describe('findGradleFiles', () => {
	it('finds Groovy and Kotlin build files', () => {
		const found = findGradleFiles(tree(['build.gradle', 'app/build.gradle.kts', 'README.md']));

		expect(found).toContain('build.gradle');
		expect(found).toContain('app/build.gradle.kts');
		expect(found).not.toContain('README.md');
	});

	it('ranks the app module ahead of the root and other modules', () => {
		const found = findGradleFiles(tree(['build.gradle', 'core/build.gradle', 'app/build.gradle']));

		expect(found[0]).toBe('app/build.gradle');
	});

	it('ranks sample and test modules last', () => {
		const found = findGradleFiles(tree(['sample/build.gradle', 'core/build.gradle']));

		expect(found[0]).toBe('core/build.gradle');
	});
});

describe('findManifestFiles', () => {
	it('prefers a main source set over flavours', () => {
		const found = findManifestFiles(
			tree(['app/src/debug/AndroidManifest.xml', 'app/src/main/AndroidManifest.xml'])
		);

		expect(found[0]).toBe('app/src/main/AndroidManifest.xml');
	});
});

describe('resolvePackageIdentity', () => {
	it('returns empty identity without a tree', async () => {
		expect(await resolvePackageIdentity(null, async () => null)).toEqual({
			packageName: null,
			versionName: null,
			versionCode: null
		});
	});

	it('reads identity from the application module', async () => {
		const files = new Map([
			[
				'app/build.gradle',
				`plugins { id "com.android.application" }
				applicationId "dev.yuki.sample"\nversionCode 7\nversionName "1.0"`
			]
		]);

		const identity = await resolvePackageIdentity(
			tree(['app/build.gradle']),
			async (path) => files.get(path) ?? null
		);

		expect(identity).toEqual({
			packageName: 'dev.yuki.sample',
			versionName: '1.0',
			versionCode: '7'
		});
	});

	it('falls back to the manifest when gradle has no applicationId', async () => {
		const files = new Map([
			['app/build.gradle', `dependencies { }`],
			['app/src/main/AndroidManifest.xml', `<manifest package="dev.yuki.frommanifest">`]
		]);

		const identity = await resolvePackageIdentity(
			tree(['app/build.gradle', 'app/src/main/AndroidManifest.xml']),
			async (path) => files.get(path) ?? null
		);

		expect(identity.packageName).toBe('dev.yuki.frommanifest');
	});

	it('ignores library modules when choosing the package', async () => {
		const files = new Map([
			[
				'app/build.gradle',
				`plugins { id "com.android.application" }
				applicationId "dev.yuki.app"`
			],
			[
				'common/build.gradle',
				`plugins { id "com.android.library" }
				android { namespace = "dev.yuki.common" }`
			]
		]);

		const identity = await resolvePackageIdentity(
			tree(['common/build.gradle', 'app/build.gradle']),
			async (path) => files.get(path) ?? null
		);

		expect(identity.packageName).toBe('dev.yuki.app');
	});

	it('finds an application module that is not named app', async () => {
		const files = new Map([
			[
				'manager/build.gradle',
				`plugins { id "com.android.application" }
				applicationId "moe.shizuku.privileged.api"`
			],
			[
				'common/build.gradle',
				`plugins { id "com.android.library" }
				android { namespace = "rikka.shizuku.common" }`
			]
		]);

		const identity = await resolvePackageIdentity(
			tree(['common/build.gradle', 'manager/build.gradle']),
			async (path) => files.get(path) ?? null
		);

		expect(identity.packageName).toBe('moe.shizuku.privileged.api');
	});

	it('prefers the primary app over a secondary application module', async () => {
		const files = new Map([
			[
				'manager/build.gradle',
				`plugins { id "com.android.application" }
				applicationId "moe.shizuku.privileged.api"`
			],
			[
				'shell/build.gradle',
				`plugins { id "com.android.application" }
				applicationId "rikka.shizuku.shell"`
			]
		]);

		const identity = await resolvePackageIdentity(
			tree(['shell/build.gradle', 'manager/build.gradle']),
			async (path) => files.get(path) ?? null
		);

		expect(identity.packageName).toBe('moe.shizuku.privileged.api');
	});

	it('skips an application module whose id is a variable', async () => {
		const files = new Map([
			[
				'app/build.gradle',
				`plugins { id "com.android.application" }
				applicationId = defaultManagerPackageName`
			]
		]);

		const identity = await resolvePackageIdentity(
			tree(['app/build.gradle']),
			async (path) => files.get(path) ?? null
		);

		expect(identity.packageName).toBeNull();
	});

	it('survives unreadable files', async () => {
		const identity = await resolvePackageIdentity(tree(['app/build.gradle']), async () => null);

		expect(identity.packageName).toBeNull();
	});
});
