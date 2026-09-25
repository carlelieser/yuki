const REQUEST_TIMEOUT_MILLIS = 30_000;
const DATA_URI = /^data:([^;,]+)((?:;[^;,]+)*),(.*)$/s;

const EXTENSION_TYPES: Record<string, string> = {
	png: 'image/png',
	webp: 'image/webp',
	jpg: 'image/jpeg',
	jpeg: 'image/jpeg',
	gif: 'image/gif',
	svg: 'image/svg+xml'
};

export type IconAsset = {
	bytes: Uint8Array;
	contentType: string;
};

export async function readIconAsset(source: string): Promise<IconAsset> {
	if (source.startsWith('data:')) return decodeDataUri(source);

	const response = await fetch(source, { signal: AbortSignal.timeout(REQUEST_TIMEOUT_MILLIS) });
	if (!response.ok) {
		throw new Error(`downloading icon ${source} failed with status ${response.status}`);
	}

	return {
		bytes: new Uint8Array(await response.arrayBuffer()),
		contentType: typeFromPath(new URL(source).pathname) ?? imageType(response, source)
	};
}

function decodeDataUri(source: string): IconAsset {
	const match = DATA_URI.exec(source);
	if (match === null) throw new Error('decoding icon data uri failed: malformed uri');

	const [, contentType = '', parameters = '', payload = ''] = match;
	const isBase64 = parameters.split(';').includes('base64');
	const bytes = isBase64
		? Buffer.from(payload, 'base64')
		: Buffer.from(decodeURIComponent(payload), 'utf8');

	return { bytes: new Uint8Array(bytes), contentType };
}

function typeFromPath(path: string): string | null {
	const extension = path.slice(path.lastIndexOf('.') + 1).toLowerCase();
	return EXTENSION_TYPES[extension] ?? null;
}

function imageType(response: Response, source: string): string {
	const header = response.headers.get('content-type')?.split(';')[0]?.trim() ?? '';
	if (header.startsWith('image/')) return header;

	throw new Error(`downloading icon ${source} failed: unexpected content type "${header}"`);
}
