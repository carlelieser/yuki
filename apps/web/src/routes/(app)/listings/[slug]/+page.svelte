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
		ItemMedia,
		ItemTitle
	} from '@yuki/ui';
	import {
		CollectionEmpty,
		DownloadButton,
		ImageCarousel,
		ListingBadges,
		ProductCard,
		RatingSummary,
		ReviewCard,
		ReviewForm,
		ReviewsDialog,
		Section,
		ShareMenu
	} from '$lib/components/storefront/index.ts';
	import type { PageData } from './$types';
	import { StarIcon, BoxIcon } from '@lucide/svelte';

	let { data }: { data: PageData } = $props();

	const listing = $derived(data.listing);
	const latestVersion = $derived(listing.versions[0]);
	const summary = $derived(data.summary);

	let reviewsOpen = $state(false);
</script>

{#snippet banner()}
	<img src={listing.bannerUrl} alt="" class="size-full object-cover" loading="lazy" />
{/snippet}

{#snippet icon()}
	<img src={listing.iconUrl} alt="" class="size-full object-cover" loading="lazy" />
{/snippet}

{#snippet badges()}
	<ListingBadges entry={listing} />
{/snippet}

<svelte:head>
	<title>{listing.title} · Yuki</title>
	{#if listing.description}
		<meta name="description" content={listing.description} />
	{/if}
</svelte:head>

<main class="mx-auto w-full max-w-4xl space-y-8 px-4 py-8">
	<ProductCard
		variant="detail"
		title={listing.title}
		description={listing.description}
		badge={listing.isArchived ? 'Archived' : undefined}
		image={listing.bannerUrl ? banner : undefined}
		icon={listing.iconUrl ? icon : undefined}
		{badges}
	/>

	<div class="flex flex-wrap items-center gap-2">
		{#if latestVersion?.downloadUrl}
			<DownloadButton
				slug={listing.slug}
				tag={latestVersion.tag}
				label="Download {latestVersion.tag}"
			/>
		{/if}
		<Button href={listing.repositoryUrl} variant="ghost">
			<GithubIcon />
			Source
		</Button>
		<ShareMenu title={listing.title} />
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
							<ItemMedia variant="icon">
								<BoxIcon />
							</ItemMedia>
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
									<DownloadButton
										slug={listing.slug}
										tag={version.tag}
										label={version.tag}
										size="sm"
									/>
								</ItemActions>
							{/if}
						</Item>
					{/each}
				</ItemGroup>
			</CollapsibleContent>
		</Section>
	{/if}

	<Section title="Ratings and reviews">
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
			<ReviewForm data={data.form} user={data.user} isEditing={data.hasReviewed} />
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
