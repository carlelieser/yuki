<script lang="ts">
	import {
		Button,
		Dialog,
		DialogClose,
		DialogContent,
		DialogDescription,
		DialogTitle
	} from '@yuki/ui';
	import ChevronLeftIcon from '@lucide/svelte/icons/chevron-left';
	import ChevronRightIcon from '@lucide/svelte/icons/chevron-right';
	import XIcon from '@lucide/svelte/icons/x';
	import { describeScreenshot, nextIndex, previousIndex } from './screenshot-navigation.ts';

	let {
		images,
		title,
		activeIndex = $bindable(0),
		isOpen = $bindable(false)
	}: {
		images: { url: string; alt: string | null }[];
		title: string;
		activeIndex: number;
		isOpen: boolean;
	} = $props();

	let contentRef = $state<HTMLElement | null>(null);

	const activeImage = $derived(images[activeIndex]);
	const hasMultiple = $derived(images.length > 1);
	const neighbourUrls = $derived(
		hasMultiple
			? [
					images[previousIndex(activeIndex, images.length)]?.url,
					images[nextIndex(activeIndex, images.length)]?.url
				].filter((url) => url !== undefined)
			: []
	);

	function goPrevious() {
		activeIndex = previousIndex(activeIndex, images.length);
	}

	function goNext() {
		activeIndex = nextIndex(activeIndex, images.length);
	}

	function focusViewer(event: Event) {
		event.preventDefault();
		contentRef?.focus();
	}

	function handleKeyDown(event: KeyboardEvent) {
		if (event.key === 'ArrowLeft') {
			event.preventDefault();
			goPrevious();
		} else if (event.key === 'ArrowRight') {
			event.preventDefault();
			goNext();
		}
	}
</script>

<Dialog bind:open={isOpen}>
	<DialogContent
		bind:ref={contentRef}
		hasCloseButton={false}
		hasZoomAnimation={false}
		overlayClass="bg-background/90"
		onkeydown={handleKeyDown}
		onOpenAutoFocus={focusViewer}
		class="start-0 top-0 flex h-dvh w-screen max-w-none translate-x-0 translate-y-0 items-center justify-center rounded-none border-none bg-transparent p-0 shadow-none"
	>
		<DialogTitle class="sr-only">Screenshots</DialogTitle>
		<DialogDescription class="sr-only">Screenshots for {title}</DialogDescription>

		{#if activeImage}
			<img
				src={activeImage.url}
				alt={describeScreenshot(activeImage.alt, activeIndex, images.length)}
				class="max-h-[85dvh] max-w-[92vw] object-contain"
				decoding="sync"
				referrerpolicy="no-referrer"
			/>
		{/if}

		{#each neighbourUrls as url (url)}
			<img
				src={url}
				alt=""
				aria-hidden="true"
				class="pointer-events-none absolute size-px opacity-0"
				referrerpolicy="no-referrer"
			/>
		{/each}

		{#if hasMultiple}
			<Button
				variant="secondary"
				size="icon-lg"
				onclick={goPrevious}
				class="absolute start-2 top-1/2 -translate-y-1/2 rounded-full sm:start-6"
			>
				<ChevronLeftIcon />
				<span class="sr-only">Previous screenshot</span>
			</Button>
			<Button
				variant="secondary"
				size="icon-lg"
				onclick={goNext}
				class="absolute end-2 top-1/2 -translate-y-1/2 rounded-full sm:end-6"
			>
				<ChevronRightIcon />
				<span class="sr-only">Next screenshot</span>
			</Button>
			<p
				aria-live="polite"
				class="absolute bottom-6 start-1/2 -translate-x-1/2 rounded-full bg-secondary px-3 py-1 text-sm text-secondary-foreground"
			>
				{activeIndex + 1} / {images.length}
			</p>
		{/if}

		<DialogClose>
			{#snippet child({ props })}
				<Button
					{...props}
					variant="secondary"
					size="icon"
					class="absolute end-4 top-4 rounded-full"
				>
					<XIcon />
					<span class="sr-only">Close viewer</span>
				</Button>
			{/snippet}
		</DialogClose>
	</DialogContent>
</Dialog>
