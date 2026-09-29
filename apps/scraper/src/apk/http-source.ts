import type { ApkSource } from './identity.ts';

const REQUEST_TIMEOUT_MILLIS = 20_000;
const GONE_STATUSES = new Set([404, 410]);

export class ApkFetchError extends Error {}

export function httpApkSource(url: string): ApkSource {
	return {
		size: async () => {
			const response = await request(url, { method: 'HEAD' });
			if (GONE_STATUSES.has(response.status)) return null;
			if (!response.ok) {
				throw new ApkFetchError(`reading the size of ${url} returned ${response.status}`);
			}

			const length = Number(response.headers.get('content-length'));
			return Number.isFinite(length) && length > 0 ? length : null;
		},
		read: async (start, end) => {
			const response = await request(url, { headers: { range: `bytes=${start}-${end}` } });
			if (response.status !== 206) {
				throw new ApkFetchError(
					`reading bytes ${start}-${end} of ${url} returned ${response.status}, not a partial response`
				);
			}

			try {
				return Buffer.from(await response.arrayBuffer());
			} catch (cause) {
				throw new ApkFetchError(`reading bytes ${start}-${end} of ${url} failed`, { cause });
			}
		}
	};
}

async function request(url: string, init: RequestInit): Promise<Response> {
	try {
		return await fetch(url, { ...init, signal: AbortSignal.timeout(REQUEST_TIMEOUT_MILLIS) });
	} catch (cause) {
		const reason = cause instanceof Error ? cause.message : String(cause);
		throw new ApkFetchError(`requesting ${url} failed: ${reason}`, { cause });
	}
}
