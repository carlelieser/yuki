export function requireRedisUrl(): string {
	const redisUrl = process.env.REDIS_URL;
	if (!redisUrl) {
		throw new Error('Missing required environment variable REDIS_URL');
	}
	return redisUrl;
}
