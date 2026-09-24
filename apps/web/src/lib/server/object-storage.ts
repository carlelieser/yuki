import { DeleteObjectCommand, PutObjectCommand, S3Client } from '@aws-sdk/client-s3';

const IMMUTABLE_CACHE_CONTROL = 'public, max-age=31536000, immutable';

type Bucket = {
	name: string;
	client: S3Client;
};

let bucket: Bucket | undefined;

function requireEnv(name: string): string {
	const value = process.env[name];
	if (!value) throw new Error(`Missing required environment variable ${name}`);
	return value;
}

function getBucket(): Bucket {
	bucket ??= {
		name: requireEnv('R2_BUCKET'),
		client: new S3Client({
			region: 'auto',
			endpoint: `https://${requireEnv('R2_ACCOUNT_ID')}.r2.cloudflarestorage.com`,
			credentials: {
				accessKeyId: requireEnv('R2_ACCESS_KEY_ID'),
				secretAccessKey: requireEnv('R2_SECRET_ACCESS_KEY')
			}
		})
	};
	return bucket;
}

function assetsPrefix(): string {
	return `${requireEnv('ASSETS_BASE_URL').replace(/\/+$/, '')}/`;
}

export function publicUrlFor(key: string): string {
	return `${assetsPrefix()}${key}`;
}

export function keyFromPublicUrl(url: string): string | null {
	const prefix = assetsPrefix();
	if (!url.startsWith(prefix)) return null;

	const key = url.slice(prefix.length);
	return key.length > 0 ? key : null;
}

export async function putObject(key: string, bytes: Buffer, contentType: string): Promise<void> {
	const { name, client } = getBucket();
	const command = new PutObjectCommand({
		Bucket: name,
		Key: key,
		Body: bytes,
		ContentType: contentType,
		CacheControl: IMMUTABLE_CACHE_CONTROL
	});

	try {
		await client.send(command);
	} catch (cause) {
		throw new Error(`Failed to upload object "${key}"`, { cause });
	}
}

export async function deleteObject(key: string): Promise<void> {
	const { name, client } = getBucket();

	try {
		await client.send(new DeleteObjectCommand({ Bucket: name, Key: key }));
	} catch (cause) {
		throw new Error(`Failed to delete object "${key}"`, { cause });
	}
}
