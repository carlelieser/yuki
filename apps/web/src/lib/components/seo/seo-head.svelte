<script lang="ts">
	import { SITE_NAME, siteUrl } from '$lib/site.ts';
	import type { PageMeta } from '$lib/seo/page-meta.ts';

	const FALLBACK_IMAGE = '/og-default.png';

	let {
		title,
		description,
		canonicalPath,
		imageUrl = null,
		isIndexable = true,
		jsonLd = null
	}: PageMeta & { jsonLd?: string | null } = $props();

	const canonical = $derived(siteUrl(canonicalPath));
	const image = $derived(imageUrl ?? siteUrl(FALLBACK_IMAGE));
	const cardType = $derived(imageUrl === null ? 'summary_large_image' : 'summary');
</script>

<svelte:head>
	<title>{title}</title>
	<meta name="description" content={description} />
	<link rel="canonical" href={canonical} />

	{#if !isIndexable}
		<meta name="robots" content="noindex,follow" />
	{/if}

	<meta property="og:type" content="website" />
	<meta property="og:site_name" content={SITE_NAME} />
	<meta property="og:title" content={title} />
	<meta property="og:description" content={description} />
	<meta property="og:url" content={canonical} />
	<meta property="og:image" content={image} />

	<meta name="twitter:card" content={cardType} />
	<meta name="twitter:title" content={title} />
	<meta name="twitter:description" content={description} />
	<meta name="twitter:image" content={image} />

	{#if jsonLd}
		<!-- eslint-disable-next-line svelte/no-at-html-tags -- jsonLd is escaped by toSoftwareApplicationSchema -->
		{@html `<script type="application/ld+json">${jsonLd}</${'script'}>`}
	{/if}
</svelte:head>
