import { getRequestEvent } from '$app/server';
import { createAuth, type Auth } from '@yuki/auth';
import { getDatabase } from './database.ts';
import { getRedisClient } from './redis.ts';

let auth: Auth | undefined;

export function getAuth(): Auth {
	auth ??= createAuth(getDatabase(), getRedisClient(), getRequestEvent);
	return auth;
}
