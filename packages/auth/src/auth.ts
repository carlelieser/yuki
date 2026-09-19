import { betterAuth } from 'better-auth';
import { drizzleAdapter } from 'better-auth/adapters/drizzle';
import { bearer } from 'better-auth/plugins';
import { sveltekitCookies } from 'better-auth/svelte-kit';
import type { Database } from '@yuki/db';
import { schema } from '@yuki/db';
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

export function createAuth(db: Database, getRequestEvent: GetRequestEvent) {
	return betterAuth({
		appName: 'Yuki',
		secret: requireAuthSecret(),
		baseURL: requireAuthUrl(),
		database: drizzleAdapter(db, { provider: 'pg', schema }),
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
				},
				architecture: {
					type: ['arm64-v8a', 'armeabi-v7a', 'x86_64', 'x86'],
					required: false
				}
			}
		},
		plugins: [bearer(), sveltekitCookies(getRequestEvent)]
	});
}

export type Auth = ReturnType<typeof createAuth>;
export type SessionData = Auth['$Infer']['Session']['session'];
export type SessionUser = Auth['$Infer']['Session']['user'];
