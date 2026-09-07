<script lang="ts">
	import {
		AspectRatio,
		Carousel,
		CarouselContent,
		CarouselItem,
		CarouselNext,
		CarouselPrevious,
		type CarouselAPI
	} from '@yuki/ui';
	import ImageViewer from './image-viewer.svelte';
	import { untrack } from 'svelte';
	import { describeScreenshot } from './screenshot-navigation.ts';

	let {
		images,
		title,
		ratio = 16 / 9
	}: {
		images: { url: string; alt: string | null }[];
		title: string;
		ratio?: number;
	} = $props();

	let isViewerOpen = $state(false);
	let activeIndex = $state(0);
	let inlineApi = $state<CarouselAPI | undefined>();
	let isReducedMotion = $state(false);

	$effect(() => {
		const query = window.matchMedia('(prefers-reduced-motion: reduce)');
		isReducedMotion = query.matches;

		function update() {
			isReducedMotion = query.matches;
		}

		query.addEventListener('change', update);
		return () => query.removeEventListener('change', update);
	});

	function openViewer(index: number) {
		activeIndex = index;
		isViewerOpen = true;
	}

	$effect(() => {
		if (isViewerOpen) return;
		untrack(() => inlineApi?.scrollTo(activeIndex));
	});
</script>

<Carousel
	opts={{ align: 'start', duration: isReducedMotion ? 0 : undefined }}
	setApi={(api) => (inlineApi = api)}
	class="-ml-1"
>
	<CarouselContent class="-ms-2">
		{#each images as image, index (index)}
			<CarouselItem class="basis-4/5 ps-2 sm:basis-1/2 lg:basis-1/3">
				<div class="p-1">
					<button
						type="button"
						onclick={() => openViewer(index)}
						aria-label="View {describeScreenshot(image.alt, index, images.length)}"
						class="group block w-full overflow-hidden rounded-lg border bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
					>
						<AspectRatio {ratio}>
							<img
								src={image.url}
								alt={describeScreenshot(image.alt, index, images.length)}
								class="size-full object-cover transition-transform duration-200 group-hover:scale-[1.02] motion-reduce:transition-none motion-reduce:group-hover:scale-100"
								loading="lazy"
								decoding="async"
								referrerpolicy="no-referrer"
							/>
						</AspectRatio>
					</button>
				</div>
			</CarouselItem>
		{/each}
	</CarouselContent>
	<CarouselPrevious class="start-6 hidden disabled:invisible sm:flex" />
	<CarouselNext class="end-6 hidden disabled:invisible sm:flex" />
</Carousel>

<ImageViewer {images} {title} bind:activeIndex bind:isOpen={isViewerOpen} />
