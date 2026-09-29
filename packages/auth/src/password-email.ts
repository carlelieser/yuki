import { and, eq, isNotNull } from 'drizzle-orm';
import type { Database } from '@yuki/db';
import { schema } from '@yuki/db';
import type { EmailContent } from './email-template.ts';

export type PasswordEmail = { subject: string; content: EmailContent };

export async function hasPassword(db: Database, userId: string): Promise<boolean> {
	const rows = await db
		.select({ id: schema.account.id })
		.from(schema.account)
		.where(
			and(
				eq(schema.account.userId, userId),
				eq(schema.account.providerId, 'credential'),
				isNotNull(schema.account.password)
			)
		)
		.limit(1);

	return rows.length > 0;
}

export function passwordEmail(isReset: boolean, url: string): PasswordEmail {
	if (isReset) {
		return {
			subject: 'Reset your Yuki password',
			content: {
				previewText: 'Choose a new password using the link inside.',
				heading: 'Reset your password',
				body: 'Choose a new password for your Yuki account using the link below.',
				action: { label: 'Reset password', url },
				footnote: 'If you did not request this, you can ignore this email.'
			}
		};
	}

	return {
		subject: 'Set a password for Yuki',
		content: {
			previewText: 'Add a password to your Yuki account using the link inside.',
			heading: 'Set a password',
			body: 'Add a password so you can sign in to Yuki with your email address as well as GitHub.',
			action: { label: 'Set password', url },
			footnote: 'If you did not request this, you can ignore this email.'
		}
	};
}
