declare global {
	namespace App {
		interface Locals {
			db: import('@yuki/db').Database;
			redis: import('@yuki/redis').RedisClient;
		}
	}
}

export {};
