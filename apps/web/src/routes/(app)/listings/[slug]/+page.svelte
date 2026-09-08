<script lang="ts">
	import { resolve } from '$app/paths';
	import {
		Badge,
		Button,
		CollapsibleContent,
		GithubIcon,
		Item,
		ItemActions,
		ItemContent,
		ItemDescription,
		ItemGroup,
		ItemTitle,
		Number
	} from '@yuki/ui';
	import {
		CollectionEmpty,
		ImageCarousel,
		RatingSummary,
		ReviewCard,
		ReviewForm,
		ReviewsDialog,
		Section
	} from '$lib/components/storefront/index.ts';
	import type { PageData } from './$types';
	import DownloadIcon from '@lucide/svelte/icons/download';
	import { SquareTextIcon, StarIcon, BoxIcon } from '@lucide/svelte';

	let { data }: { data: PageData } = $props();

	const listing = $derived(data.listing);
	const latestVersion = $derived(listing.versions[0]);
	const summary = $derived(data.summary);

	let reviewsOpen = $state(false);
</script>

<svelte:head>
	<title>{listing.title} — Yuki</title>
	{#if listing.description}
		<meta name="description" content={listing.description} />
	{/if}
</svelte:head>

<main class="mx-auto w-full max-w-4xl space-y-8 px-4 py-8">
	{#if listing.bannerUrl}
		<img
			src={listing.bannerUrl}
			alt=""
			class="h-auto w-full rounded-xl border object-cover"
			decoding="async"
			referrerpolicy="no-referrer"
		/>
	{/if}

	<header class="flex flex-wrap items-start gap-4">
		{#if listing.iconUrl}
			<img
				src={listing.iconUrl}
				alt=""
				class="size-16 shrink-0 rounded-xl border object-cover"
				loading="lazy"
			/>
		{:else}
			<div class="size-16 shrink-0 rounded-xl border bg-muted">
				<BoxIcon />
			</div>
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
	</header>

	<div class="flex items-center flex-wrap gap-2">
		{#if latestVersion?.downloadUrl}
			<Button
				href={resolve('/(app)/listings/[slug]/download/[tag]', {
					slug: listing.slug,
					tag: latestVersion.tag
				})}
				data-sveltekit-preload-data="off"
				rel="nofollow"
			>
				<DownloadIcon />
				Download {latestVersion.tag}
			</Button>
		{/if}
		<Button href={listing.repositoryUrl} variant="ghost">
			<GithubIcon />
			Source
		</Button>
		<div class="flex items-center gap-2 ml-auto">
			<Badge variant="ghost">
				<StarIcon />
				<Number value={listing.stars} preset="compact" /> stars
			</Badge>
			{#if listing.license}
				<Badge variant="ghost">
					<SquareTextIcon />
					{listing.license}
				</Badge>
			{/if}
			{#if listing.isArchived}
				<Badge variant="ghost">Archived</Badge>
			{/if}
		</div>
	</div>

	{#if listing.screenshots.length > 0}
		<Section title="Screenshots">
			<ImageCarousel images={listing.screenshots} title={listing.title} />
		</Section>
	{/if}

	{#if listing.versions.length > 0}
		<Section title="Releases">
			<CollapsibleContent showMoreLabel="Show all releases" showLessLabel="Show fewer releases">
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
									<Button
										href={resolve('/(app)/listings/[slug]/download/[tag]', {
											slug: listing.slug,
											tag: version.tag
										})}
										data-sveltekit-preload-data="off"
										rel="nofollow"
										variant="ghost"
									>
										<DownloadIcon />
										Download
									</Button>
								</ItemActions>
							{/if}
						</Item>
					{/each}
				</ItemGroup>
			</CollapsibleContent>
		</Section>
	{/if}

	<Section title="Ratings & reviews">
		{#snippet action()}
			{#if data.reviews.hasMore}
				<Button variant="ghost" onclick={() => (reviewsOpen = true)}>Show all reviews</Button>
			{/if}
		{/snippet}

		<RatingSummary {summary} />

		{#if data.user === null}
			<p class="text-sm text-muted-foreground">
				<a
					href={resolve('/(auth)/signin')}
					class="underline underline-offset-4"
					data-sveltekit-preload-data="off">Sign in</a
				> to write a review.
			</p>
		{:else if data.canReview}
			<ReviewForm data={data.form} isEditing={data.hasReviewed} />
		{:else}
			<p class="text-sm text-muted-foreground">Download this app to write a review.</p>
		{/if}

		{#if summary.total === 0}
			<CollectionEmpty
				title="No reviews yet"
				description="Be the first to share what you think of this app."
			>
				{#snippet icon()}
					<StarIcon />
				{/snippet}
			</CollectionEmpty>
		{:else}
			<div class="space-y-3">
				{#each data.reviews.results as review (review.id)}
					<ReviewCard {review} />
				{/each}
			</div>
		{/if}
	</Section>
</main>

<ReviewsDialog
	slug={listing.slug}
	initial={data.reviews.results}
	total={summary.total}
	bind:open={reviewsOpen}
/>
