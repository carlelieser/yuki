function optional(name: string): string | undefined {
	const value = process.env[name];
	return value && value.length > 0 ? value : undefined;
}

export function requireAuthSecret(): string {
	const secret = process.env.BETTER_AUTH_SECRET;
	if (!secret) {
		throw new Error('Missing required environment variable BETTER_AUTH_SECRET');
	}
	return secret;
}

export function requireAuthUrl(): string {
	const url = process.env.BETTER_AUTH_URL;
	if (!url) {
		throw new Error('Missing required environment variable BETTER_AUTH_URL');
	}
	return url;
}

export type OAuthCredentials = { clientId: string; clientSecret: string };

function oauthCredentials(prefix: string): OAuthCredentials | undefined {
	const clientId = optional(`${prefix}_CLIENT_ID`);
	const clientSecret = optional(`${prefix}_CLIENT_SECRET`);
	return clientId && clientSecret ? { clientId, clientSecret } : undefined;
}

export function getGithubCredentials(): OAuthCredentials | undefined {
	return oauthCredentials('GITHUB');
}

export function getGoogleCredentials(): OAuthCredentials | undefined {
	return oauthCredentials('GOOGLE');
}

export type SmtpConfig = {
	host: string;
	port: number;
	from: string;
	user?: string;
	pass?: string;
};

export function getSmtpConfig(): SmtpConfig | undefined {
	const host = optional('SMTP_HOST');
	if (!host) return undefined;

	const port = Number(optional('SMTP_PORT') ?? '1025');
	if (!Number.isInteger(port) || port <= 0) {
		throw new Error('Invalid environment variable SMTP_PORT');
	}

	return {
		host,
		port,
		from: optional('SMTP_FROM') ?? 'no-reply@yuki.local',
		user: optional('SMTP_USER'),
		pass: optional('SMTP_PASS')
	};
}
