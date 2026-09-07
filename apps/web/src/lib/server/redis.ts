import { createRedisClient, type RedisClient } from '@yuki/redis';

let redisClient: RedisClient | undefined;

export function getRedisClient(): RedisClient {
	redisClient ??= createRedisClient();
	return redisClient;
}
