import { createTransport, type Transporter } from 'nodemailer';
import { getSmtpConfig, type SmtpConfig } from './env.ts';

export type MailMessage = { to: string; subject: string; text: string };

let transporter: Transporter | undefined;
let transporterFor: SmtpConfig | undefined;

function getTransporter(config: SmtpConfig): Transporter {
	if (!transporter || transporterFor !== config) {
		transporter = createTransport({
			host: config.host,
			port: config.port,
			secure: false,
			auth: config.user && config.pass ? { user: config.user, pass: config.pass } : undefined
		});
		transporterFor = config;
	}
	return transporter;
}

export async function sendMail(message: MailMessage): Promise<void> {
	const config = getSmtpConfig();

	if (!config) {
		console.info(
			`[auth] SMTP not configured, logging mail instead\nto: ${message.to}\nsubject: ${message.subject}\n${message.text}`
		);
		return;
	}

	try {
		await getTransporter(config).sendMail({
			from: config.from,
			to: message.to,
			subject: message.subject,
			text: message.text
		});
	} catch (cause) {
		const reason = cause instanceof Error ? cause.message : String(cause);
		console.error(`[auth] failed to send mail to ${message.to}: ${reason}\n${message.text}`);
	}
}
