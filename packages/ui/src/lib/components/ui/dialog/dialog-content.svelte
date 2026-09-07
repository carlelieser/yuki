<script lang="ts">
	import { Dialog as DialogPrimitive } from 'bits-ui';
	import XIcon from '@lucide/svelte/icons/x';
	import { cn, type WithoutChildrenOrChild } from '../../../cn.ts';
	import DialogOverlay from './dialog-overlay.svelte';
	import DialogPortal from './dialog-portal.svelte';
	import type { ComponentProps } from 'svelte';

	let {
		ref = $bindable(null),
		class: className,
		overlayClass,
		hasCloseButton = true,
		hasZoomAnimation = true,
		portalProps,
		children,
		...restProps
	}: DialogPrimitive.ContentProps & {
		overlayClass?: string;
		hasCloseButton?: boolean;
		hasZoomAnimation?: boolean;
		portalProps?: WithoutChildrenOrChild<ComponentProps<typeof DialogPortal>>;
	} = $props();
</script>

<DialogPortal {...portalProps}>
	<DialogOverlay class={overlayClass} />
	<DialogPrimitive.Content
		bind:ref
		data-slot="dialog-content"
		class={cn(
			'fixed start-1/2 top-1/2 z-50 grid w-full max-w-lg -translate-x-1/2 -translate-y-1/2 gap-4 rounded-lg border bg-background p-6 shadow-lg duration-200 outline-none data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=closed]:animate-out data-[state=closed]:fade-out-0',
			hasZoomAnimation && 'data-[state=open]:zoom-in-95 data-[state=closed]:zoom-out-95',
			className
		)}
		{...restProps}
	>
		{@render children?.()}
		{#if hasCloseButton}
			<DialogPrimitive.Close
				class="absolute end-4 top-4 rounded-sm opacity-70 transition-opacity hover:opacity-100 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
			>
				<XIcon class="size-4" />
				<span class="sr-only">Close</span>
			</DialogPrimitive.Close>
		{/if}
	</DialogPrimitive.Content>
</DialogPortal>
