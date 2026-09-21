import { decodePng, type RgbaImage } from './png.ts';

const WEBP_RIFF = 'RIFF';
const WEBP_FORM = 'WEBP';

export function isWebp(source: Buffer): boolean {
	if (source.length < 12) return false;

	return (
		source.toString('ascii', 0, 4) === WEBP_RIFF && source.toString('ascii', 8, 12) === WEBP_FORM
	);
}

declare const WebAssembly: {
	compile(bytes: Uint8Array): Promise<object>;
};

async function compileWasm(bytes: Uint8Array): Promise<never> {
	return WebAssembly.compile(bytes) as Promise<never>;
}

let webpReady: Promise<void> | null = null;

async function initWebp(): Promise<void> {
	const [{ init }, { readFile }] = await Promise.all([
		import('@jsquash/webp/decode.js'),
		import('node:fs/promises')
	]);

	const wasm = await readFile(
		new URL('../../node_modules/@jsquash/webp/codec/dec/webp_dec.wasm', import.meta.url)
	);

	await init(await compileWasm(wasm));
}

async function decodeWebp(source: Buffer): Promise<RgbaImage | null> {
	const { default: decode } = await import('@jsquash/webp/decode.js');

	webpReady ??= initWebp();
	await webpReady;

	const buffer = source.buffer.slice(
		source.byteOffset,
		source.byteOffset + source.byteLength
	) as ArrayBuffer;

	const image = await decode(buffer).catch(() => null);
	if (image === null) return null;

	return {
		width: image.width,
		height: image.height,
		pixels: Buffer.from(image.data.buffer, image.data.byteOffset, image.data.byteLength)
	};
}

export async function decodeRaster(source: Buffer): Promise<RgbaImage | null> {
	return isWebp(source) ? decodeWebp(source) : decodePng(source);
}
