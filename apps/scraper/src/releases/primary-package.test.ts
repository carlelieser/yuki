import { describe, expect, it } from 'vitest';
import { primaryPackage, type PackagedAsset } from './primary-package.ts';

const repository = { githubRepoId: 1, owner: 'meowarex', name: 'rl-mobile' };

function build(versionId: string, packageName: string, signer = 'own'): PackagedAsset {
	const day = Number(versionId.replace(/\D/g, '')) || 1;
	return {
		versionId,
		packageName,
		signer,
		publishedAt: new Date(Date.UTC(2026, 0, day)),
		isPrerelease: false
	};
}

describe('primaryPackage', () => {
	it('picks the package the repository ships in the most releases', () => {
		const assets = [
			build('v1', 'top.weixiansen574.hybridfilexfer'),
			build('v2', 'top.weixiansen574.hybridfilexfer'),
			build('v2', 'com.antutu.ABenchMark')
		];

		expect(primaryPackage(assets, { ...repository, owner: 'qqq', name: 'zzz' })).toBe(
			'top.weixiansen574.hybridfilexfer'
		);
	});

	it('prefers the package named after the repository when releases tie', () => {
		const assets = [
			build('v1', 'com.aspiro.tidal', 'tidal'),
			build('v1', 'com.meowarex.rlmobile'),
			build('v2', 'com.aspiro.tidal', 'tidal'),
			build('v2', 'com.meowarex.rlmobile')
		];

		expect(primaryPackage(assets, repository)).toBe('com.meowarex.rlmobile');
	});

	it("prefers the listing's usual signer when nothing else separates them", () => {
		const assets = [
			build('v1', 'org.example.aid', 'other'),
			build('v2', 'org.example.aid', 'other'),
			build('v1', 'org.example.alpha'),
			build('v1', 'org.example.alpha'),
			build('v2', 'org.example.alpha')
		];

		expect(primaryPackage(assets, { ...repository, owner: 'none', name: 'none' })).toBe(
			'org.example.alpha'
		);
	});

	it('prefers the base package over its variants', () => {
		const assets = [
			build('v1', 'dev.imranr.obtainium.fdroid'),
			build('v1', 'dev.imranr.obtainium')
		];

		expect(primaryPackage(assets, repository)).toBe('dev.imranr.obtainium');
	});

	it('passes over debug packages unless they are all there is', () => {
		expect(
			primaryPackage(
				[
					build('v1', 'com.acme.app.debug'),
					build('v2', 'com.acme.app.debug'),
					build('v2', 'com.acme.app')
				],
				repository
			)
		).toBe('com.acme.app');
		expect(primaryPackage([build('v1', 'com.acme.app.debug')], repository)).toBe(
			'com.acme.app.debug'
		);
	});

	it('follows an override for a repository whose main app the rule cannot tell', () => {
		const simpleWear = { githubRepoId: 197253270, owner: 'SimpleAppProjects', name: 'SimpleWear' };
		const assets = [
			build('v1', 'com.thewizrd.wearsettings'),
			build('v2', 'com.thewizrd.wearsettings'),
			build('v2', 'com.thewizrd.simplewear')
		];

		expect(primaryPackage(assets, simpleWear)).toBe('com.thewizrd.simplewear');
	});

	it('has no main package for a listing without identified apks', () => {
		expect(primaryPackage([], repository)).toBeNull();
	});

	it('does not treat a very short owner name as a match', () => {
		const assets = [
			build('v1', 'com.example.app'),
			build('v1', 'com.example.tablet'),
			build('v2', 'com.example.app'),
			build('v2', 'com.example.tablet')
		];

		expect(primaryPackage(assets, { githubRepoId: 2, owner: 'ab', name: 'zzz' })).toBe(
			'com.example.app'
		);
	});

	it('follows a rename once the newest release ships the new package', () => {
		const assets = [
			build('v1', 'eu.kanade.tachiyomi.y2k'),
			build('v2', 'eu.kanade.tachiyomi.y2k'),
			build('v3', 'eu.kanade.tachiyomi.y2k'),
			build('v4', 'app.reikai')
		];

		expect(primaryPackage(assets, repository)).toBe('app.reikai');
	});

	it('looks past a newer prerelease to the newest stable release', () => {
		const assets = [
			build('v1', 'it.palsoftware.pastiera'),
			{ ...build('v2', 'it.palsoftware.pastiera.nightly'), isPrerelease: true }
		];

		expect(primaryPackage(assets, repository)).toBe('it.palsoftware.pastiera');
	});
});
