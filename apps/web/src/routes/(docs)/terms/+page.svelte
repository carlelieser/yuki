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
	<title>Terms of Service · Yuki</title>
	<meta name="description" content="The terms governing your use of Yuki." />
</svelte:head>

<h1>Terms of Service</h1>

<p class="lead">Effective {LEGAL_EFFECTIVE_DATE}</p>

<p>
	These Terms of Service ("Terms") are a binding agreement between you and {LEGAL_OPERATOR}, an
	individual operating the Yuki website and service ("Yuki", "we", "us"). By accessing Yuki or
	creating an account, you agree to these Terms. If you do not agree, do not use Yuki.
</p>

<h2>1. What Yuki is</h2>

<p>
	Yuki is a catalog of open-source Android applications that use the Shizuku permission framework.
	We index publicly available repositories hosted on GitHub and present information about them,
	including descriptions, categories, and links to release files published by those projects.
</p>

<p>
	<strong>Yuki does not host, build, sign, or distribute application binaries.</strong> When you download
	an application, you are downloading a file published by a third-party developer on GitHub. We are not
	the author, publisher, or distributor of that software, and we do not review it for security, functionality,
	or safety.
</p>

<h2>2. Eligibility</h2>

<p>
	You must be at least 13 years old to use Yuki. If you are under the age of majority where you
	live, you may use Yuki only with the involvement of a parent or legal guardian. By using Yuki, you
	represent that you meet these requirements.
</p>

<h2>3. Your account</h2>

<p>
	Some features require an account. You may register with an email address and password, or through
	GitHub or Google. You agree to provide accurate information, to keep your credentials
	confidential, and to be responsible for all activity that occurs under your account. Notify us at
	<a href="mailto:{LEGAL_CONTACT_EMAIL}">{LEGAL_CONTACT_EMAIL}</a> if you believe your account has been
	compromised.
</p>

<p>
	We verify email addresses before activating accounts. You may close your account at any time by
	writing to us at <a href="mailto:{LEGAL_CONTACT_EMAIL}">{LEGAL_CONTACT_EMAIL}</a>.
</p>

<h2>4. Reviews and user content</h2>

<p>
	Yuki lets you post reviews and ratings ("User Content"). You retain ownership of your User
	Content. By posting it, you grant us a worldwide, non-exclusive, royalty-free license to host,
	store, display, reproduce, and distribute that content in connection with operating and promoting
	Yuki. This license ends when you delete your content, except for copies retained in backups for a
	reasonable period.
</p>

<p>You agree not to post User Content that:</p>

<ul>
	<li>is unlawful, defamatory, harassing, hateful, or threatening;</li>
	<li>infringes anyone's intellectual property or privacy rights;</li>
	<li>contains malware, exploit code, or links intended to cause harm;</li>
	<li>is spam, advertising, or deliberately manipulates ratings; or</li>
	<li>impersonates another person or misrepresents your affiliation.</li>
</ul>

<p>
	We may remove User Content and suspend or terminate accounts that violate these Terms, at our
	discretion and without prior notice. We are under no obligation to monitor User Content, but we
	may do so.
</p>

<h2>5. Acceptable use</h2>

<p>You agree not to:</p>

<ul>
	<li>scrape, crawl, or harvest data from Yuki except through documented interfaces;</li>
	<li>
		interfere with or overload our infrastructure, including through automated request volume;
	</li>
	<li>probe, scan, or attempt to breach authentication or access controls;</li>
	<li>reverse engineer or circumvent any technical limitation of the service; or</li>
	<li>use Yuki to violate any applicable law or the rights of others.</li>
</ul>

<h2>6. Third-party software and links</h2>

<p>
	Applications listed on Yuki are published by independent developers under their own licenses and
	terms. Your use of any such application is governed by that developer's license and privacy
	practices, not these Terms. You are solely responsible for evaluating any software before
	installing it.
</p>

<p>
	Applications using Shizuku request elevated permissions on your device. Installing software from
	outside an official app store carries risk, including the risk of data loss or device compromise.
	You assume that risk entirely.
</p>

<p>
	Yuki links to and retrieves data from GitHub. We do not control third-party services and are not
	responsible for their content, availability, or practices.
</p>

<h2>7. Intellectual property</h2>

<p>
	The Yuki name, logo, site design, and original text are owned by {LEGAL_OPERATOR} and protected by intellectual
	property law. Listing metadata describing third-party projects remains the property of its respective
	owners. Nothing in these Terms grants you a license to our branding.
</p>

<h2>8. Copyright complaints</h2>

<p>
	If you believe material on Yuki infringes your copyright, send a notice to
	<a href="mailto:{LEGAL_CONTACT_EMAIL}">{LEGAL_CONTACT_EMAIL}</a> that identifies the copyrighted work,
	identifies the material you want removed and where it appears, provides your contact information, and
	includes a statement made under penalty of perjury that you are authorized to act on the owner's behalf
	and believe in good faith that the use is unauthorized. We remove infringing material and terminate
	repeat infringers.
</p>

<h2>9. Availability and changes</h2>

<p>
	Yuki is provided on an as-available basis. We may modify, suspend, or discontinue any part of the
	service at any time, and we may add or remove listings at our discretion. We do not guarantee that
	listings are complete, current, or accurate.
</p>

<h2>10. Disclaimer of warranties</h2>

<p>
	YUKI IS PROVIDED "AS IS" AND "AS AVAILABLE" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS,
	IMPLIED, OR STATUTORY, INCLUDING THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT. WE DO NOT WARRANT THAT THE SERVICE WILL BE
	UNINTERRUPTED, SECURE, OR ERROR-FREE, OR THAT ANY SOFTWARE LISTED ON YUKI IS FREE OF HARMFUL
	COMPONENTS.
</p>

<p>
	Some jurisdictions do not allow the exclusion of implied warranties, so some of the above may not
	apply to you.
</p>

<h2>11. Limitation of liability</h2>

<p>
	TO THE MAXIMUM EXTENT PERMITTED BY LAW, {LEGAL_OPERATOR} WILL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL,
	SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES, OR FOR ANY LOSS OF PROFITS, DATA, OR DEVICE FUNCTIONALITY,
	ARISING OUT OF OR RELATING TO YOUR USE OF YUKI OR ANY SOFTWARE OBTAINED THROUGH IT, EVEN IF ADVISED
	OF THE POSSIBILITY OF SUCH DAMAGES.
</p>

<p>
	OUR TOTAL AGGREGATE LIABILITY FOR ALL CLAIMS RELATING TO YUKI WILL NOT EXCEED ONE HUNDRED U.S.
	DOLLARS ($100).
</p>

<p>
	Some jurisdictions do not allow the limitation of liability for certain damages, so some of the
	above may not apply to you.
</p>

<h2>12. Indemnification</h2>

<p>
	You agree to indemnify and hold harmless {LEGAL_OPERATOR} from any claim, demand, loss, or expense (including
	reasonable legal fees) arising out of your User Content, your use of Yuki, or your violation of these
	Terms or of any law or third-party right.
</p>

<h2>13. Termination</h2>

<p>
	You may stop using Yuki at any time. We may suspend or terminate your access if you breach these
	Terms or if we discontinue the service. Sections 4 through 15 survive termination.
</p>

<h2>14. Governing law and disputes</h2>

<p>
	These Terms are governed by the laws of the State of {LEGAL_GOVERNING_STATE}, United States,
	without regard to its conflict-of-laws rules. You and we agree that any dispute arising out of or
	relating to these Terms or to Yuki will be brought exclusively in the state or federal courts
	located in {LEGAL_GOVERNING_STATE}, and you consent to personal jurisdiction and venue there.
</p>

<p>
	If you are a consumer resident in the European Economic Area or the United Kingdom, nothing in
	this section deprives you of the protection of mandatory consumer law in your country of
	residence, or of the right to bring proceedings in your local courts.
</p>

<h2>15. Changes to these Terms</h2>

<p>
	We may update these Terms. When we make material changes, we will update the effective date above
	and, where you have an account, notify you by email before the changes take effect. Continued use
	of Yuki after that date means you accept the revised Terms.
</p>

<h2>16. General</h2>

<p>
	These Terms, together with our <a href={resolve('/(docs)/privacy')}>Privacy Policy</a>, are the
	entire agreement between you and us regarding Yuki. If any provision is held unenforceable, the
	rest remain in effect. Our failure to enforce a provision is not a waiver of it. You may not
	assign these Terms; we may assign them in connection with a transfer of the service.
</p>

<h2>17. Contact</h2>

<p>
	Questions about these Terms can be sent to
	<a href="mailto:{LEGAL_CONTACT_EMAIL}">{LEGAL_CONTACT_EMAIL}</a>.
</p>
