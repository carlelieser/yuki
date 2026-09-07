<script lang="ts">
	import {
		Badge,
		Button,
		Item,
		ItemActions,
		ItemContent,
		ItemDescription,
		ItemGroup,
		ItemTitle,
		Separator
	} from '@yuki/ui';
	import { Section } from '$lib/components/storefront/index.ts';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();

	const listing = $derived(data.listing);
	const latestVersion = $derived(listing.versions[0]);
</script>

<svelte:head>
	<title>{listing.title} — Yuki</title>
	{#if listing.description}
		<meta name="description" content={listing.description} />
	{/if}
</svelte:head>

<main class="mx-auto w-full max-w-4xl space-y-8 px-4 py-8">
	<header class="flex flex-wrap items-start gap-4">
		{#if listing.iconUrl}
			<img
				src={listing.iconUrl}
				alt=""
				class="size-16 shrink-0 rounded-xl border object-cover"
				loading="lazy"
			/>
		{:else}
			<div class="size-16 shrink-0 rounded-xl border bg-muted"></div>
		{/if}

		<div class="min-w-0 flex-1 space-y-1">
			<h1 class="text-2xl font-semibold tracking-tight">{listing.title}</h1>
			<p class="text-sm text-muted-foreground">
				by <a href={listing.authorUrl} rel="external noreferrer" class="hover:underline"
					>{listing.author}</a
				>
			</p>
			{#if listing.description}
				<p class="text-sm text-muted-foreground">{listing.description}</p>
			{/if}
		</div>

		<div class="flex items-center gap-2">
			{#if latestVersion?.downloadUrl}
				<Button href={latestVersion.downloadUrl}>Download {latestVersion.tag}</Button>
			{/if}
			<Button href={listing.repositoryUrl} variant="outline">Source</Button>
		</div>
	</header>

	<div class="flex flex-wrap gap-2 text-sm">
		<Badge variant="secondary">{listing.stars} stars</Badge>
		{#if listing.license}
			<Badge variant="secondary">{listing.license}</Badge>
		{/if}
		{#if listing.isArchived}
			<Badge variant="outline">Archived</Badge>
		{/if}
	</div>

	<Separator />

	{#if listing.screenshots.length > 0}
		<Section title="Screenshots">
			<ul class="flex snap-x gap-4 overflow-x-auto pb-2">
				{#each listing.screenshots as screenshot (screenshot.url)}
					<li class="snap-start">
						<img
							src={screenshot.url}
							alt={screenshot.alt ?? ''}
							class="h-80 w-auto rounded-lg border object-cover"
							loading="lazy"
						/>
					</li>
				{/each}
			</ul>
		</Section>
	{/if}

	{#if listing.versions.length > 0}
		<Section title="Releases">
			<ItemGroup>
				{#each listing.versions as version (version.tag)}
					<Item variant="outline">
						<ItemContent>
							<ItemTitle>
								{version.name ?? version.tag}
								{#if version.isPrerelease}
									<Badge variant="outline">Prerelease</Badge>
								{/if}
							</ItemTitle>
							{#if version.publishedAt}
								<ItemDescription>{version.publishedAt.toLocaleDateString()}</ItemDescription>
							{/if}
						</ItemContent>
						{#if version.downloadUrl}
							<ItemActions>
								<Button href={version.downloadUrl} variant="outline" size="sm">
									Download
								</Button>
							</ItemActions>
						{/if}
					</Item>
				{/each}
			</ItemGroup>
		</Section>
	{/if}
</main>
