<script lang="ts">
	import { SiteLogo } from '$lib/components/storefront';
	import authIllustration from '$lib/assets/auth-illustration.png?enhanced';
	import ModeToggle from '$lib/components/storefront/mode-toggle.svelte';

	let { children } = $props();
</script>

<svelte:head>
	<meta name="robots" content="noindex,follow" />
</svelte:head>

<div class="grid min-h-svh lg:grid-cols-2">
	<div class="flex flex-col">
		<header class="flex items-center justify-between px-6 py-6 lg:px-10">
			<SiteLogo showLabel={false} size="md" />
			<ModeToggle />
		</header>

		<div class="flex flex-1 flex-col justify-center px-6 pb-12 lg:px-10">
			<div class="mx-auto w-full max-w-sm">
				{@render children?.()}
			</div>
		</div>
	</div>

	<div class="hidden items-center justify-center bg-[#1e44af] p-12 lg:flex">
		<enhanced:img
			src={authIllustration}
			alt=""
			aria-hidden="true"
			sizes="(min-width: 1024px) 50vw"
			class="max-h-[min(80svh,40rem)] w-full max-w-md object-contain"
		/>
	</div>
</div>
