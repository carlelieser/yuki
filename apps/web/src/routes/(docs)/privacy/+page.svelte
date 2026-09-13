<script lang="ts">
	import { resolve } from '$app/paths';
	import {
		LEGAL_CONTACT_EMAIL,
		LEGAL_EFFECTIVE_DATE,
		LEGAL_GOVERNING_STATE,
		LEGAL_OPERATOR
	} from '$lib/legal.ts';
</script>

<svelte:head>
	<title>Privacy Policy · Yuki</title>
	<meta name="description" content="How Yuki collects, uses, and protects your personal data." />
</svelte:head>

<h1>Privacy Policy</h1>

<p class="lead">Effective {LEGAL_EFFECTIVE_DATE}</p>

<p>
	This policy explains how {LEGAL_OPERATOR} ("we", "us"), operating Yuki, collects and handles personal
	data. We are the data controller for the information described here.
</p>

<p>
	<strong>In short:</strong> we collect the minimum needed to run an account and show you reviews. We
	do not use analytics or advertising trackers, we do not sell your data, and we do not share it with
	third parties for their own purposes.
</p>

<h2>1. Information we collect</h2>

<h3>Account information</h3>

<p>When you register, we store:</p>

<ul>
	<li>your name and email address;</li>
	<li>a hashed password, if you register with email and password;</li>
	<li>
		your profile image and provider account identifier, if you register through GitHub or Google;
	</li>
	<li>whether your email address has been verified; and</li>
	<li>the dates your account was created and last updated.</li>
</ul>

<p>
	We never store your password in readable form. If you sign in with GitHub, we request access to
	your email address only. If you sign in with Google, we receive your basic profile and email. We
	do not receive your password from either provider.
</p>

<h3>Content you submit</h3>

<p>
	If you write a review, we store its rating, text, and timestamps, linked to your account. Reviews
	are public and display your name and profile image.
</p>

<h3>Preferences</h3>

<p>
	We store your preferred device architecture on your account, and your preferred catalog view mode
	in a cookie on your device, so the site behaves consistently between visits.
</p>

<h3>Technical information</h3>

<p>
	Our servers process standard request data, including IP address, browser user agent, and requested
	pages, as an inherent part of delivering the site and protecting it from abuse. We do not combine
	this into advertising or behavioral profiles.
</p>

<h3>What we do not collect</h3>

<p>
	We do not use analytics services, advertising networks, tracking pixels, or third-party cookies.
	We do not track your activity across other websites. We do not collect precise location data, and
	we have no access to anything on your Android device.
</p>

<h2>2. Cookies</h2>

<p>We use cookies that are strictly necessary to operate the site:</p>

<ul>
	<li>
		<strong>Session cookies</strong> keep you signed in and protect against cross-site request forgery.
		They are cleared when you sign out.
	</li>
	<li>
		<strong>Preference cookies</strong> remember display choices such as view mode and light or dark theme.
	</li>
</ul>

<p>
	Because these cookies are essential to providing a service you have requested, they do not require
	consent under the ePrivacy Directive. We set no advertising or analytics cookies. You can clear
	cookies in your browser, though doing so will sign you out.
</p>

<h2>3. Why we use your data, and our legal basis</h2>

<table>
	<thead>
		<tr>
			<th>Purpose</th>
			<th>Legal basis (GDPR)</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>Creating and maintaining your account</td>
			<td>Performance of a contract</td>
		</tr>
		<tr>
			<td>Authenticating you and keeping sessions secure</td>
			<td>Performance of a contract</td>
		</tr>
		<tr>
			<td>Sending verification and password reset emails</td>
			<td>Performance of a contract</td>
		</tr>
		<tr>
			<td>Publishing the reviews you choose to post</td>
			<td>Performance of a contract</td>
		</tr>
		<tr>
			<td>Remembering your display preferences</td>
			<td>Legitimate interests</td>
		</tr>
		<tr>
			<td>Preventing abuse, spam, and unauthorized access</td>
			<td>Legitimate interests</td>
		</tr>
		<tr>
			<td>Complying with legal obligations</td>
			<td>Legal obligation</td>
		</tr>
	</tbody>
</table>

<p>
	We send only transactional email — address verification and password resets. We do not send
	marketing email.
</p>

<h2>4. Who we share data with</h2>

<p>
	<strong>We do not sell your personal data, and we have never sold it.</strong> We do not share it with
	third parties for their own marketing. We disclose data only to:
</p>

<ul>
	<li>
		<strong>Infrastructure providers</strong> that host our servers, database, and email delivery, acting
		as processors on our instructions and bound to protect your data;
	</li>
	<li>
		<strong>Authentication providers</strong> (GitHub, Google) when you choose to sign in through them,
		limited to completing that sign-in; and
	</li>
	<li>
		<strong>Legal authorities</strong>, where we are required by valid legal process, or where
		disclosure is necessary to protect our rights or someone's safety.
	</li>
</ul>

<p>
	If the service is ever transferred to another operator, your data may transfer with it. We will
	give notice before that happens and before any different policy applies to you.
</p>

<p>
	Reviews you post are public by design and visible to anyone, including search engines. Please do
	not include personal information in review text.
</p>

<h2>5. International transfers</h2>

<p>
	We operate from the United States, and our infrastructure providers may process data there or in
	other countries. Where we transfer personal data out of the European Economic Area or the United
	Kingdom, we rely on the European Commission's Standard Contractual Clauses, or the UK
	International Data Transfer Addendum, to provide an appropriate level of protection. You may
	request details of these safeguards using the contact address below.
</p>

<h2>6. How long we keep data</h2>

<ul>
	<li><strong>Account data</strong> is kept while your account is open.</li>
	<li>
		<strong>Reviews</strong> are kept until you or we delete them. If you delete your account, your reviews
		are deleted with it.
	</li>
	<li><strong>Sessions</strong> expire automatically and are removed when you sign out.</li>
	<li>
		<strong>Backups</strong> containing deleted data are overwritten in the normal backup rotation, within
		30 days.
	</li>
</ul>

<p>
	After you request deletion, we remove your personal data within 30 days, except where we must
	retain something to comply with a legal obligation or to resolve a dispute.
</p>

<h2>7. Security</h2>

<p>
	We protect data in transit with TLS, hash passwords using a slow, salted algorithm, verify email
	addresses before activating accounts, and restrict database access. No system is perfectly secure,
	but if a breach affects your personal data and poses a risk to you, we will notify you and the
	relevant supervisory authority as required by law.
</p>

<h2>8. Your rights</h2>

<p>
	We extend the following rights to <strong>all users, wherever you live</strong>, rather than only
	where law compels it:
</p>

<ul>
	<li><strong>Access</strong> — obtain a copy of the personal data we hold about you.</li>
	<li><strong>Correction</strong> — have inaccurate data corrected.</li>
	<li><strong>Deletion</strong> — have your account and personal data erased.</li>
	<li><strong>Portability</strong> — receive your data in a machine-readable format.</li>
	<li>
		<strong>Objection and restriction</strong> — object to or limit processing based on legitimate interests.
	</li>
	<li>
		<strong>Withdraw consent</strong> — where processing relies on consent, withdraw it at any time.
	</li>
	<li>
		<strong>Non-discrimination</strong> — we will not degrade your service or charge you differently for
		exercising these rights.
	</li>
</ul>

<p>
	To exercise any of these, email
	<a href="mailto:{LEGAL_CONTACT_EMAIL}">{LEGAL_CONTACT_EMAIL}</a> from the address on your account. We
	respond within 30 days and will ask for verification before acting on a request. You may use an authorized
	agent to submit a request on your behalf.
</p>

<p>
	Because we do not sell or share personal data for cross-context behavioral advertising, there is
	no opt-out to offer under California law, and we do not process sensitive personal information for
	purposes requiring a right to limit.
</p>

<p>
	If you are in the European Economic Area or the United Kingdom, you also have the right to lodge a
	complaint with your local data protection authority. We would appreciate the chance to address
	your concern first.
</p>

<h2>9. Children</h2>

<p>
	Yuki is not directed at children under 13, and we do not knowingly collect their personal data. If
	you believe a child has given us personal data, write to
	<a href="mailto:{LEGAL_CONTACT_EMAIL}">{LEGAL_CONTACT_EMAIL}</a> and we will delete it.
</p>

<h2>10. Third-party services</h2>

<p>
	Yuki links to GitHub and to files published there. When you follow a link or download a release,
	that request goes to GitHub and is subject to GitHub's privacy policy, not this one. Applications
	you install handle their own data under their developers' terms. We have no visibility into or
	control over those practices.
</p>

<h2>11. Changes to this policy</h2>

<p>
	We may update this policy. When changes are material, we will update the effective date above and
	notify account holders by email before they take effect. Previous versions are available on
	request.
</p>

<h2>12. Contact</h2>

<p>
	{LEGAL_OPERATOR}, {LEGAL_GOVERNING_STATE}, United States<br />
	<a href="mailto:{LEGAL_CONTACT_EMAIL}">{LEGAL_CONTACT_EMAIL}</a>
</p>

<p>See also our <a href={resolve('/(docs)/terms')}>Terms of Service</a>.</p>
