import { describe, expect, it } from 'vitest';
import { renderEmail, type EmailContent } from './email-template.ts';

const content: EmailContent = {
	previewText: 'Confirm your email address to finish setting up Yuki',
	heading: 'Confirm your email address',
	body: 'Confirm this address to finish setting up your Yuki account.',
	action: { label: 'Verify email', url: 'https://yukistore.org/verify-email?token=abc' }
};

describe('renderEmail', () => {
	it('escapes the ampersand separating query parameters, which would otherwise truncate the link', () => {
		const { html } = renderEmail({
			...content,
			action: { label: 'Verify email', url: 'https://yukistore.org/v?token=abc&next=/library' }
		});

		expect(html).toContain('token=abc&amp;next=/library');
		expect(html).not.toMatch(/[^;]&next=/);
	});

	it('escapes markup in content so a crafted name cannot inject script into the email', () => {
		const { html } = renderEmail({
			...content,
			heading: '<script>alert(1)</script>'
		});

		expect(html).not.toContain('<script>');
		expect(html).toContain('&lt;script&gt;');
	});

	it('escapes quotes in the action url so it cannot break out of the href attribute', () => {
		const { html } = renderEmail({
			...content,
			action: { label: 'Verify email', url: 'https://yukistore.org/v"onmouseover="alert(1)' }
		});

		expect(html).not.toContain('"onmouseover="');
		expect(html).toContain('&quot;onmouseover=&quot;');
	});

	it('shows the logo when given an origin', () => {
		const { html } = renderEmail(content, 'https://yukistore.org');

		expect(html).toContain('src="https://yukistore.org/logo.png"');
	});

	it('falls back to a text wordmark without an origin, so no client shows a broken image', () => {
		const { html } = renderEmail(content);

		expect(html).not.toContain('<img');
		expect(html).toContain('>Yuki</span>');
	});

	it('labels the link in the text body, which is all a text-only client renders', () => {
		const { text } = renderEmail(content);

		expect(text).toContain('Verify email:\nhttps://yukistore.org/verify-email?token=abc');
	});

	it('leaves the url unescaped in the text body, where entities would be shown literally', () => {
		const { text } = renderEmail({
			...content,
			action: { label: 'Verify email', url: 'https://yukistore.org/v?token=abc&next=/library' }
		});

		expect(text).toContain('token=abc&next=/library');
		expect(text).not.toContain('&amp;');
	});

	it('omits the footnote row when there is none, rather than leaving an empty block', () => {
		const withFootnote = renderEmail({ ...content, footnote: 'Ignore this if unexpected.' });
		const withoutFootnote = renderEmail(content);

		expect(withFootnote.html).toContain('Ignore this if unexpected.');
		expect(withFootnote.text).toContain('Ignore this if unexpected.');
		expect(withoutFootnote.text.endsWith('?token=abc\n')).toBe(true);
	});

	it('carries dark mode rules for both the media query and the attribute Outlook.com rewrites to', () => {
		const { html } = renderEmail(content);

		expect(html).toContain('@media (prefers-color-scheme:dark)');
		expect(html).toContain('[data-ogsc] .yk-card');
	});

	it('keeps light colours inline so clients that strip the style block stay styled', () => {
		const { html } = renderEmail(content);

		expect(html).toContain('background-color:#ffffff');
		expect(html).toContain('color:#0f172a');
	});
});
