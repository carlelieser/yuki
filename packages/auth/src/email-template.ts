const palette = {
	background: '#f4f6fb',
	surface: '#ffffff',
	border: '#e6efff',
	primary: '#2466ea',
	onPrimary: '#ffffff',
	heading: '#0f172a',
	body: '#475569',
	muted: '#94a3b8'
};

const darkPalette = {
	background: '#0a0a0a',
	surface: '#171717',
	border: '#2a2a2a',
	primary: '#6da2ff',
	onPrimary: '#171717',
	heading: '#fafafa',
	body: '#d4d4d4',
	muted: '#a1a1a1'
};

const font = "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif";

export type EmailAction = { label: string; url: string };

export type EmailContent = {
	previewText: string;
	heading: string;
	body: string;
	action: EmailAction;
	footnote?: string;
};

export type RenderedEmail = { html: string; text: string };

const darkRules = [
	['.yk-bg', `background-color:${darkPalette.background} !important;`],
	[
		'.yk-card',
		`background-color:${darkPalette.surface} !important;border-color:${darkPalette.border} !important;`
	],
	['.yk-heading', `color:${darkPalette.heading} !important;`],
	['.yk-body', `color:${darkPalette.body} !important;`],
	['.yk-muted', `color:${darkPalette.muted} !important;`],
	['.yk-link', `color:${darkPalette.primary} !important;`],
	[
		'.yk-button',
		`background-color:${darkPalette.primary} !important;color:${darkPalette.onPrimary} !important;`
	]
];

function darkStyles(): string {
	const body = darkRules.map(([selector, rules]) => `${selector}{${rules}}`).join('');
	const inverted = darkRules
		.map(([selector, rules]) => `[data-ogsc] ${selector}{${rules}}`)
		.join('');

	return `<style>@media (prefers-color-scheme:dark){${body}}${inverted}</style>`;
}

function escapeHtml(value: string): string {
	return value
		.replaceAll('&', '&amp;')
		.replaceAll('<', '&lt;')
		.replaceAll('>', '&gt;')
		.replaceAll('"', '&quot;')
		.replaceAll("'", '&#39;');
}

function logoMarkup(origin: string | undefined): string {
	const wordmark = `<span class="yk-heading" style="font-family:${font};font-size:20px;font-weight:600;letter-spacing:-0.01em;color:${palette.heading};">Yuki</span>`;
	if (!origin) return wordmark;

	return `<img src="${escapeHtml(`${origin}/logo.png`)}" alt="Yuki" width="40" height="40" style="display:block;width:40px;height:40px;border:0;outline:none;text-decoration:none;" />`;
}

function buttonMarkup(action: EmailAction): string {
	const url = escapeHtml(action.url);
	return `<a class="yk-button" href="${url}" style="display:inline-block;padding:12px 28px;background-color:${palette.primary};color:${palette.onPrimary};font-family:${font};font-size:15px;font-weight:600;line-height:20px;text-decoration:none;border-radius:8px;">${escapeHtml(action.label)}</a>`;
}

function footnoteMarkup(footnote: string | undefined): string {
	if (!footnote) return '';
	return `<tr><td class="yk-muted" style="padding:0 40px 32px;font-family:${font};font-size:13px;line-height:20px;color:${palette.muted};">${escapeHtml(footnote)}</td></tr>`;
}

function renderHtml(content: EmailContent, origin: string | undefined): string {
	const url = escapeHtml(content.action.url);

	return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1" />
<meta name="color-scheme" content="light dark" />
<meta name="supported-color-schemes" content="light dark" />
<title>${escapeHtml(content.heading)}</title>
${darkStyles()}
</head>
<body class="yk-bg" style="margin:0;padding:0;background-color:${palette.background};">
<div style="display:none;max-height:0;overflow:hidden;opacity:0;">${escapeHtml(content.previewText)}</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" class="yk-bg" style="background-color:${palette.background};">
<tr><td align="center" style="padding:40px 16px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" class="yk-card" style="max-width:520px;background-color:${palette.surface};border:1px solid ${palette.border};border-radius:12px;">
<tr><td style="padding:32px 40px 24px;">${logoMarkup(origin)}</td></tr>
<tr><td class="yk-heading" style="padding:0 40px 16px;font-family:${font};font-size:22px;font-weight:600;line-height:30px;letter-spacing:-0.01em;color:${palette.heading};">${escapeHtml(content.heading)}</td></tr>
<tr><td class="yk-body" style="padding:0 40px 28px;font-family:${font};font-size:15px;line-height:24px;color:${palette.body};">${escapeHtml(content.body)}</td></tr>
<tr><td style="padding:0 40px 28px;">${buttonMarkup(content.action)}</td></tr>
<tr><td class="yk-muted" style="padding:0 40px 32px;font-family:${font};font-size:13px;line-height:20px;color:${palette.muted};">Or paste this link into your browser:<br /><a class="yk-link" href="${url}" style="color:${palette.primary};text-decoration:underline;word-break:break-all;">${url}</a></td></tr>
${footnoteMarkup(content.footnote)}
</table>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="max-width:520px;">
<tr><td class="yk-muted" style="padding:24px 40px;font-family:${font};font-size:12px;line-height:18px;color:${palette.muted};text-align:center;">Yuki</td></tr>
</table>
</td></tr>
</table>
</body>
</html>`;
}

function renderText(content: EmailContent): string {
	const lines = [
		content.heading,
		'',
		content.body,
		'',
		`${content.action.label}:`,
		content.action.url
	];
	if (content.footnote) lines.push('', content.footnote);
	return `${lines.join('\n')}\n`;
}

export function renderEmail(content: EmailContent, origin?: string): RenderedEmail {
	return { html: renderHtml(content, origin), text: renderText(content) };
}
