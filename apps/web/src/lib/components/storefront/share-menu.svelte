<script lang="ts">
	import { page } from '$app/state';
	import {
		Button,
		Dialog,
		DialogContent,
		DialogDescription,
		DialogTitle,
		DropdownMenu,
		DropdownMenuContent,
		DropdownMenuItem,
		DropdownMenuTrigger
	} from '@yuki/ui';
	import QR from '@svelte-put/qr/svg/QR.svelte';
	import { toast } from 'svelte-sonner';
	import { copyToClipboard, shareUrl } from './share-menu.ts';
	import LinkIcon from '@lucide/svelte/icons/link';
	import QrCodeIcon from '@lucide/svelte/icons/qr-code';
	import Share2Icon from '@lucide/svelte/icons/share-2';
	import type { Snippet } from 'svelte';

	type ShareMenuTriggerProps = { props: Record<string, unknown> };

	let { title, trigger }: { title: string; trigger?: Snippet<[ShareMenuTriggerProps]> } = $props();

	let isQrOpen = $state(false);

	const url = $derived(shareUrl(page.url));

	async function copy(): Promise<void> {
		const result = await copyToClipboard(
			url,
			typeof navigator === 'undefined' ? undefined : navigator.clipboard
		);

		if (result.ok) {
			toast.success('Link copied');
			return;
		}

		toast.error('Could not copy the link');
	}
</script>

<DropdownMenu>
	<DropdownMenuTrigger>
		{#snippet child({ props })}
			{#if trigger}
				{@render trigger({ props })}
			{:else}
				<Button {...props} variant="ghost" aria-label="Share {title}">
					<Share2Icon aria-hidden="true" />
					Share
				</Button>
			{/if}
		{/snippet}
	</DropdownMenuTrigger>

	<DropdownMenuContent align="end" class="w-44">
		<DropdownMenuItem onSelect={copy}>
			<LinkIcon aria-hidden="true" />
			Copy link
		</DropdownMenuItem>
		<DropdownMenuItem onSelect={() => (isQrOpen = true)}>
			<QrCodeIcon aria-hidden="true" />
			QR
		</DropdownMenuItem>
	</DropdownMenuContent>
</DropdownMenu>

<Dialog bind:open={isQrOpen}>
	<DialogContent hasCloseButton={false} class="max-w-xs">
		<DialogTitle class="sr-only">QR code for {title}</DialogTitle>
		<DialogDescription class="sr-only">
			Scan this QR code to open {title} at {url}
		</DialogDescription>

		<div class="flex justify-center">
			<QR
				data={url}
				shape="circle"
				correction="H"
				logo="/logo.png"
				logoRatio={1}
				moduleFill="#000000"
				anchorOuterFill="#000000"
				anchorInnerFill="#000000"
				aria-hidden="true"
				class="size-64 rounded-lg bg-white p-3"
			/>
		</div>
	</DialogContent>
</Dialog>
