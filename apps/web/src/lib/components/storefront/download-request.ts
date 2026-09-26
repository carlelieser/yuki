export type DownloadResult = { ok: true; url: string } | { ok: false; message: string };

const OFFLINE_MESSAGE = 'Check your connection and try again.';
const FALLBACK_MESSAGE = 'Something went wrong. Try again.';

async function failureMessage(response: Response): Promise<string> {
	const body: unknown = await response.json().catch(() => null);
	const message = (body as { message?: unknown } | null)?.message;

	return typeof message === 'string' ? message : FALLBACK_MESSAGE;
}

export async function requestDownload(
	endpoint: string,
	fetchImpl: typeof fetch
): Promise<DownloadResult> {
	let response: Response;
	try {
		response = await fetchImpl(endpoint, { headers: { accept: 'application/json' } });
	} catch (error) {
		if (!(error instanceof TypeError)) throw error;
		return { ok: false, message: OFFLINE_MESSAGE };
	}

	if (!response.ok) return { ok: false, message: await failureMessage(response) };

	const body: { url: string } = await response.json();
	return { ok: true, url: body.url };
}

export type ClickModifiers = Pick<MouseEvent, 'button' | 'metaKey' | 'ctrlKey' | 'shiftKey'>;

export function isPlainClick(event: ClickModifiers): boolean {
	const hasModifier = event.metaKey || event.ctrlKey || event.shiftKey;

	return event.button === 0 && !hasModifier;
}
