import Redis from 'ioredis';
import { requireRedisUrl } from './env.ts';

export type RedisClient = Redis;

export function createRedisClient(connectionString: string = requireRedisUrl()): RedisClient {
	return new Redis(connectionString, { maxRetriesPerRequest: 3, lazyConnect: true });
}
