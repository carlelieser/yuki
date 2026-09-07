declare global {
	namespace App {
		interface Locals {
			db: import('@yuki/db').Database;
			redis: import('@yuki/redis').RedisClient;
			user: import('@yuki/auth').SessionUser | null;
			session: import('@yuki/auth').SessionData | null;
		}
	}
}

export {};
