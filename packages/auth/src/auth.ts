import { betterAuth } from 'better-auth';
import { drizzleAdapter } from 'better-auth/adapters/drizzle';
import { bearer } from 'better-auth/plugins';
import { sveltekitCookies } from 'better-auth/svelte-kit';
import type { Database } from '@yuki/db';
import { schema } from '@yuki/db';
import { renderEmail, type EmailContent } from './email-template.ts';
import {
	getEmailAssetOrigin,
	getGithubCredentials,
	getGoogleCredentials,
	requireAuthSecret,
	requireAuthUrl
} from './env.ts';
import { sendMail } from './mailer.ts';

type GetRequestEvent = Parameters<typeof sveltekitCookies>[0];

async function sendTemplatedMail(to: string, subject: string, content: EmailContent) {
	const { html, text } = renderEmail(content, getEmailAssetOrigin());
	await sendMail({ to, subject, text, html });
}

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
				await sendTemplatedMail(user.email, 'Reset your Yuki password', {
					previewText: 'Reset your Yuki password',
					heading: 'Reset your password',
					body: 'Choose a new password for your Yuki account using the link below.',
					action: { label: 'Reset password', url },
					footnote: 'If you did not request this, you can ignore this email.'
				});
			}
		},
		emailVerification: {
			sendOnSignUp: true,
			autoSignInAfterVerification: true,
			callbackURL: '/verify-email',
			sendVerificationEmail: async ({ user, url }) => {
				await sendTemplatedMail(user.email, 'Verify your Yuki email address', {
					previewText: 'Confirm your email address to finish setting up Yuki',
					heading: 'Confirm your email address',
					body: 'Confirm this address to finish setting up your Yuki account.',
					action: { label: 'Verify email', url }
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
		advanced: {
			ipAddress: { ipAddressHeaders: ['x-forwarded-for'] }
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
