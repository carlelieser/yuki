import { describe, expect, it, vi } from 'vitest';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';
import type { PackageIndexEntry } from '$lib/server/package-index.ts';

const { getPackageIndex } = vi.hoisted(() => ({ getPackageIndex: vi.fn() }));

vi.mock('$lib/server/package-index.ts', () => ({ getPackageIndex }));

const { GET: packages } = await import('./+server.ts');

const db = {} as Database;

function entry(overrides: Partial<PackageIndexEntry> = {}): PackageIndexEntry {
	return {
		packageName: 'dev.imranr.obtainium.fdroid',
		githubRepoId: 4242,
		slug: 'imranr98-obtainium',
		title: 'Obtainium',
		iconUrl: 'https://example.com/icon.png',
		...overrides
	};
}

function packagesEvent() {
	const setHeaders = vi.fn();
	const event = {
		locals: { db },
		url: new URL('http://localhost/api/packages'),
		setHeaders
	} as unknown as RequestEvent;

	return { event, setHeaders };
}

describe('GET /api/packages', () => {
	it('returns the package index', async () => {
		getPackageIndex.mockResolvedValue([entry()]);

		const { event } = packagesEvent();
		const response = await packages(event);
		const body = (await response.json()) as { packages: PackageIndexEntry[] };

		expect(response.status).toBe(200);
		expect(body.packages).toHaveLength(1);
		expect(body.packages[0]?.packageName).toBe('dev.imranr.obtainium.fdroid');
		expect(body.packages[0]?.githubRepoId).toBe(4242);
	});

	it('returns an empty array when no listing has a package name', async () => {
		getPackageIndex.mockResolvedValue([]);

		const { event } = packagesEvent();
		const response = await packages(event);

		await expect(response.json()).resolves.toEqual({ packages: [] });
	});

	it('carries the slug and title so a match can be rendered offline', async () => {
		getPackageIndex.mockResolvedValue([entry()]);

		const { event } = packagesEvent();
		const response = await packages(event);
		const body = (await response.json()) as { packages: PackageIndexEntry[] };

		expect(body.packages[0]?.slug).toBe('imranr98-obtainium');
		expect(body.packages[0]?.title).toBe('Obtainium');
	});

	it('sets a cache-control header', async () => {
		getPackageIndex.mockResolvedValue([]);

		const { event, setHeaders } = packagesEvent();
		await packages(event);

		expect(setHeaders).toHaveBeenCalledWith({ 'cache-control': 'public, max-age=3600' });
	});
});
