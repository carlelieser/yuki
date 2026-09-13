<script lang="ts">
	import DocsHeader from '$lib/components/storefront/docs-header.svelte';
	import SiteFooter from '$lib/components/storefront/site-footer.svelte';

	let { children } = $props();
</script>

<div class="flex min-h-svh flex-col">
	<DocsHeader />

	<main class="mx-auto w-full max-w-3xl flex-1 px-4 py-12">
		<article
			class="prose prose-neutral max-w-none dark:prose-invert prose-headings:tracking-tight prose-h1:text-3xl prose-h2:mt-10 prose-h2:text-xl prose-a:underline-offset-4"
		>
			{@render children?.()}
		</article>
	</main>

	<SiteFooter width="narrow" />
</div>
