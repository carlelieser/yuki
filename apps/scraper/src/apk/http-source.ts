import type { ApkSource } from './package-name.ts';

const REQUEST_TIMEOUT_MILLIS = 20_000;

export function httpApkSource(url: string): ApkSource {
	return {
		size: async () => {
			const response = await request(url, { method: 'HEAD' });
			if (!response.ok) return null;

			const length = Number(response.headers.get('content-length'));
			return Number.isFinite(length) && length > 0 ? length : null;
		},
		read: async (start, end) => {
			const response = await request(url, { headers: { range: `bytes=${start}-${end}` } });
			if (response.status !== 206) {
				throw new Error(`expected a partial response, got ${response.status}`);
			}

			return Buffer.from(await response.arrayBuffer());
		}
	};
}

async function request(url: string, init: RequestInit): Promise<Response> {
	return fetch(url, { ...init, signal: AbortSignal.timeout(REQUEST_TIMEOUT_MILLIS) });
}
