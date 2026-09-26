import type { Architecture } from '@yuki/github';

type Failure = { ok: false; message: string };

type JsonResult<Body> = { ok: true; body: Body } | Failure;

export type DownloadResult = { ok: true; url: string } | Failure;

export type ArchitecturesResult = { ok: true; architectures: Architecture[] } | Failure;

const OFFLINE_MESSAGE = 'Check your connection and try again.';
const FALLBACK_MESSAGE = 'Something went wrong. Try again.';

async function failureMessage(response: Response): Promise<string> {
	const body: unknown = await response.json().catch(() => null);
	const message = (body as { message?: unknown } | null)?.message;

	return typeof message === 'string' ? message : FALLBACK_MESSAGE;
}

async function fetchJson<Body>(
	endpoint: string,
	fetchImpl: typeof fetch
): Promise<JsonResult<Body>> {
	let response: Response;
	try {
		response = await fetchImpl(endpoint, { headers: { accept: 'application/json' } });
	} catch (error) {
		if (!(error instanceof TypeError)) throw error;
		return { ok: false, message: OFFLINE_MESSAGE };
	}

	if (!response.ok) return { ok: false, message: await failureMessage(response) };

	return { ok: true, body: (await response.json()) as Body };
}

export async function requestDownload(
	endpoint: string,
	fetchImpl: typeof fetch
): Promise<DownloadResult> {
	const result = await fetchJson<{ url: string }>(endpoint, fetchImpl);

	return result.ok ? { ok: true, url: result.body.url } : result;
}

export async function requestArchitectures(
	endpoint: string,
	fetchImpl: typeof fetch
): Promise<ArchitecturesResult> {
	const result = await fetchJson<{ architectures: Architecture[] }>(endpoint, fetchImpl);

	return result.ok ? { ok: true, architectures: result.body.architectures } : result;
}

export type ClickModifiers = Pick<MouseEvent, 'button' | 'metaKey' | 'ctrlKey' | 'shiftKey'>;

export function isPlainClick(event: ClickModifiers): boolean {
	const hasModifier = event.metaKey || event.ctrlKey || event.shiftKey;

	return event.button === 0 && !hasModifier;
}
