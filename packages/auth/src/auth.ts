import { betterAuth } from 'better-auth';
import { drizzleAdapter } from 'better-auth/adapters/drizzle';
import { sveltekitCookies } from 'better-auth/svelte-kit';
import type { Database } from '@yuki/db';
import { schema } from '@yuki/db';
import type { RedisClient } from '@yuki/redis';
import {
	getGithubCredentials,
	getGoogleCredentials,
	requireAuthSecret,
	requireAuthUrl
} from './env.ts';
import { sendMail } from './mailer.ts';

type GetRequestEvent = Parameters<typeof sveltekitCookies>[0];

function socialProviders() {
	const github = getGithubCredentials();
	const google = getGoogleCredentials();

	return {
		...(github ? { github: { ...github, scope: ['user:email'] } } : {}),
		...(google ? { google } : {})
	};
}

export function createAuth(db: Database, redis: RedisClient, getRequestEvent: GetRequestEvent) {
	return betterAuth({
		appName: 'Yuki',
		secret: requireAuthSecret(),
		baseURL: requireAuthUrl(),
		database: drizzleAdapter(db, { provider: 'pg', schema }),
		secondaryStorage: {
			get: async (key) => await redis.get(key),
			getAndDelete: async (key) => await redis.getdel(key),
			increment: async (key, ttl) => {
				const results = await redis.multi().incr(key).expire(key, ttl, 'NX').exec();
				const value = results?.[0]?.[1];
				return typeof value === 'number' ? value : Number(value);
			},
			set: async (key, value, ttl) => {
				if (ttl) {
					await redis.set(key, value, 'EX', ttl);
				} else {
					await redis.set(key, value);
				}
			},
			delete: async (key) => {
				await redis.del(key);
			}
		},
		emailAndPassword: {
			enabled: true,
			requireEmailVerification: true,
			sendResetPassword: async ({ user, url }) => {
				await sendMail({
					to: user.email,
					subject: 'Reset your Yuki password',
					text: `Reset your password by opening this link:\n\n${url}\n\nIf you did not request this, you can ignore this email.`
				});
			}
		},
		emailVerification: {
			sendOnSignUp: true,
			autoSignInAfterVerification: true,
			callbackURL: '/verify-email',
			sendVerificationEmail: async ({ user, url }) => {
				await sendMail({
					to: user.email,
					subject: 'Verify your Yuki email address',
					text: `Confirm your email address by opening this link:\n\n${url}`
				});
			}
		},
		socialProviders: socialProviders(),
		account: {
			accountLinking: { enabled: true, trustedProviders: ['github', 'google'] }
		},
		session: {
			cookieCache: { enabled: true, maxAge: 300 }
		},
		user: {
			additionalFields: {
				role: {
					type: ['user', 'developer'],
					required: false,
					defaultValue: 'user',
					input: false
				}
			}
		},
		plugins: [sveltekitCookies(getRequestEvent)]
	});
}

export type Auth = ReturnType<typeof createAuth>;
export type SessionData = Auth['$Infer']['Session']['session'];
export type SessionUser = Auth['$Infer']['Session']['user'];
