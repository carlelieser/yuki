import type { R2Config } from './r2-bucket.ts';

const DEFAULT_EXTRACTOR_BINARY = 'extract-icons';

export type IconConfig = {
	extractorBinary: string;
	assetsBaseUrl: string;
	r2: R2Config;
};

export function requireIconConfig(): IconConfig {
	return {
		extractorBinary: process.env.APK_ICON_BIN || DEFAULT_EXTRACTOR_BINARY,
		assetsBaseUrl: requireVariable('ASSETS_BASE_URL'),
		r2: {
			endpoint: process.env.R2_ENDPOINT || r2Endpoint(requireVariable('R2_ACCOUNT_ID')),
			accessKeyId: requireVariable('R2_ACCESS_KEY_ID'),
			secretAccessKey: requireVariable('R2_SECRET_ACCESS_KEY'),
			bucket: requireVariable('R2_BUCKET')
		}
	};
}

function r2Endpoint(accountId: string): string {
	return `https://${accountId}.r2.cloudflarestorage.com`;
}

function requireVariable(name: string): string {
	const value = process.env[name];
	if (!value) {
		throw new Error(`Missing required environment variable ${name}`);
	}
	return value;
}
