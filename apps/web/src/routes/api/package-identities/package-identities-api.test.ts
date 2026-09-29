import { describe, expect, it, vi } from 'vitest';
import type { RequestEvent } from './$types';
import type { Database } from '@yuki/db';
import type { PackageIdentityEntry } from '$lib/server/package-index.ts';

const { getPackageIdentities } = vi.hoisted(() => ({ getPackageIdentities: vi.fn() }));

vi.mock('$lib/server/package-index.ts', () => ({ getPackageIdentities }));

const { GET: identities } = await import('./+server.ts');

const db = {} as Database;

function entry(): PackageIdentityEntry {
	return {
		packageName: 'dev.imranr.obtainium',
		githubRepoId: 4242,
		slug: 'imranr98-obtainium',
		title: 'Obtainium',
		iconUrl: null,
		identities: [{ signers: ['b353'], lineage: [] }]
	};
}

function identitiesEvent() {
	const setHeaders = vi.fn();
	const event = {
		locals: { db },
		url: new URL('http://localhost/api/package-identities'),
		setHeaders
	} as unknown as RequestEvent;

	return { event, setHeaders };
}

describe('GET /api/package-identities', () => {
	it('returns each package with the signing identities it may carry', async () => {
		getPackageIdentities.mockResolvedValue([entry()]);

		const { event } = identitiesEvent();
		const response = await identities(event);

		expect(response.status).toBe(200);
		await expect(response.json()).resolves.toEqual({ packages: [entry()] });
	});

	it('sets a cache-control header', async () => {
		getPackageIdentities.mockResolvedValue([]);

		const { event, setHeaders } = identitiesEvent();
		await identities(event);

		expect(setHeaders).toHaveBeenCalledWith({ 'cache-control': 'public, max-age=3600' });
	});
});
