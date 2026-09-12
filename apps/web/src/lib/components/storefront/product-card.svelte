<script lang="ts" module>
	import type { ViewMode } from '$lib/view-mode.ts';

	export type ProductCardVariant = 'default' | 'wide' | 'list' | 'detail';

	export function cardVariantFor(mode: ViewMode): Exclude<ProductCardVariant, 'detail' | 'wide'> {
		return mode === 'list' ? 'list' : 'default';
	}
</script>

<script lang="ts">
	import { AspectRatio, Badge, Card, CardContent } from '@yuki/ui';
	import BoxIcon from '@lucide/svelte/icons/box';
	import type { ResolvedPathname } from '$app/types';
	import type { Snippet } from 'svelte';

	type LinkedProps = {
		variant?: Exclude<ProductCardVariant, 'detail'>;
		href: ResolvedPathname;
	};

	type DetailProps = {
		variant: 'detail';
		href?: never;
	};

	let {
		title,
		href,
		badge,
		description,
		image,
		icon,
		badges,
		variant = 'default'
	}: (LinkedProps | DetailProps) & {
		title: string;
		badge?: string;
		description?: string | null;
		image?: Snippet;
		icon?: Snippet;
		badges?: Snippet;
	} = $props();
</script>

{#if variant === 'detail'}
	<div class="flex flex-col gap-4">
		<div class="relative">
			<AspectRatio ratio={16 / 9}>
				{#if image}
					<div class="size-full overflow-hidden rounded-xl border">{@render image()}</div>
				{:else}
					<div
						class="flex size-full items-center justify-center rounded-xl bg-muted text-muted-foreground"
					>
						<BoxIcon class="size-10" />
					</div>
				{/if}
			</AspectRatio>
			{#if badge}
				<Badge class="absolute start-2 top-2">{badge}</Badge>
			{/if}
		</div>
		<div class="flex items-start gap-3">
			<div
				class="flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-muted text-muted-foreground"
			>
				{#if icon}
					{@render icon()}
				{:else}
					<BoxIcon class="size-7" />
				{/if}
			</div>
			<div class="flex min-w-0 flex-1 flex-col gap-2">
				<div class="flex flex-col gap-1">
					<h1 class="text-2xl font-semibold tracking-tight">{title}</h1>
					{#if description}
						<p class="text-sm text-muted-foreground">{description}</p>
					{/if}
				</div>
				{@render badges?.()}
			</div>
		</div>
	</div>
{:else if variant === 'list'}
	<Card class="group/card h-full overflow-hidden py-0">
		<a {href} class="flex h-full items-start gap-3 p-3 focus-visible:outline-none">
			<div
				class="flex size-12 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-muted text-muted-foreground"
			>
				{#if icon}
					{@render icon()}
				{:else}
					<BoxIcon class="size-6" />
				{/if}
			</div>
			<div class="flex h-full min-w-0 flex-1 flex-col gap-1.5">
				<div class="flex flex-1 flex-col gap-0.5">
					<h3 class="line-clamp-1 text-sm font-medium group-hover/card:underline">{title}</h3>
					{#if description}
						<p class="line-clamp-1 text-xs text-muted-foreground sm:line-clamp-2">{description}</p>
					{/if}
				</div>
				{@render badges?.()}
			</div>
		</a>
	</Card>
{:else}
	<Card class="group/card h-full overflow-hidden py-0">
		<a {href} class="flex h-full flex-col focus-visible:outline-none">
			<div class="relative">
				<AspectRatio ratio={variant === 'wide' ? 16 / 9 : 1}>
					{#if image}
						{@render image()}
					{:else}
						<div class="flex size-full items-center justify-center bg-muted text-muted-foreground">
							<BoxIcon class="size-8" />
						</div>
					{/if}
				</AspectRatio>
				{#if badge}
					<Badge class="absolute start-2 top-2">{badge}</Badge>
				{/if}
			</div>
			{#if variant === 'wide'}
				<CardContent class="flex flex-1 items-start gap-3 p-3">
					<div
						class="flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-muted text-muted-foreground"
					>
						{#if icon}
							{@render icon()}
						{:else}
							<BoxIcon class="size-5" />
						{/if}
					</div>
					<div class="flex h-full min-w-0 flex-1 flex-col gap-2">
						<div class="flex flex-1 flex-col gap-0.5">
							<h3 class="line-clamp-1 text-sm font-medium group-hover/card:underline">{title}</h3>
							{#if description}
								<p class="line-clamp-1 text-xs text-muted-foreground">{description}</p>
							{/if}
						</div>
						{@render badges?.()}
					</div>
				</CardContent>
			{:else}
				<CardContent class="flex flex-1 flex-col gap-2 p-3">
					<div class="flex flex-1 flex-col gap-0.5">
						<h3 class="line-clamp-2 text-sm font-medium group-hover/card:underline">{title}</h3>
						{#if description}
							<p class="line-clamp-2 text-xs text-muted-foreground">{description}</p>
						{/if}
					</div>
					{@render badges?.()}
				</CardContent>
			{/if}
		</a>
	</Card>
{/if}
