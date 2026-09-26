<script lang="ts">
	import { resolve } from '$app/paths';
	import {
		Button,
		ButtonGroup,
		ButtonGroupSeparator,
		DropdownMenu,
		DropdownMenuContent,
		DropdownMenuGroup,
		DropdownMenuGroupHeading,
		DropdownMenuRadioGroup,
		DropdownMenuRadioItem,
		DropdownMenuTrigger,
		Spinner,
		type ButtonSize
	} from '@yuki/ui';
	import type { Architecture } from '@yuki/github';
	import { getArchitectureStore } from '$lib/architecture-store.svelte.ts';
	import { selectedArchitecture } from '$lib/architecture-preference.ts';
	import DownloadIcon from '@lucide/svelte/icons/download';
	import ChevronDownIcon from '@lucide/svelte/icons/chevron-down';
	import { toast } from 'svelte-sonner';
	import { isPlainClick, requestDownload } from './download-request.ts';

	const DEFAULT_VALUE = 'default';

	let {
		slug,
		tag,
		label,
		size = 'default'
	}: {
		slug: string;
		tag: string;
		label: string;
		size?: ButtonSize;
	} = $props();

	const store = getArchitectureStore();

	let open = $state(false);
	let hasRequested = $state(false);
	let isLoading = $state(false);
	let architectures = $state<Architecture[] | null>(null);
	let isResolving = $state(false);

	const selected = $derived(selectedArchitecture(store.preference, architectures) ?? DEFAULT_VALUE);

	const downloadPath = $derived(resolve('/(app)/listings/[slug]/download/[tag]', { slug, tag }));
	const downloadApiPath = $derived(resolve('/api/listings/[slug]/download/[tag]', { slug, tag }));

	function withArchitecture(path: string, architecture: string): string {
		if (architecture === DEFAULT_VALUE) return path;
		return `${path}?arch=${encodeURIComponent(architecture)}`;
	}

	function urlFor(architecture: string): string {
		return withArchitecture(downloadPath, architecture);
	}

	async function download(event: MouseEvent): Promise<void> {
		if (!isPlainClick(event)) return;
		event.preventDefault();
		if (isResolving) return;

		isResolving = true;
		const result = await requestDownload(withArchitecture(downloadApiPath, selected), fetch);
		isResolving = false;

		if (result.ok) {
			window.location.assign(result.url);
			return;
		}

		toast.error('Download failed', { description: result.message });
	}

	function choose(value: string): void {
		store.choose(value === DEFAULT_VALUE ? null : (value as Architecture));
	}

	const href = $derived(urlFor(selected));

	async function loadArchitectures(): Promise<void> {
		hasRequested = true;
		isLoading = true;

		try {
			const endpoint = resolve('/api/listings/[slug]/architectures/[tag]', { slug, tag });
			const response = await fetch(endpoint);
			if (!response.ok) {
				hasRequested = false;
				return;
			}

			const body: { architectures: Architecture[] } = await response.json();
			architectures = body.architectures;
		} catch {
			hasRequested = false;
		} finally {
			isLoading = false;
		}
	}

	$effect(() => {
		if (open && !hasRequested) void loadArchitectures();
	});
</script>

<ButtonGroup>
	<Button
		{href}
		{size}
		onclick={download}
		aria-busy={isResolving}
		data-sveltekit-preload-data="off"
		rel="nofollow"
	>
		{#if isResolving}
			<Spinner class="size-4" />
		{:else}
			<DownloadIcon />
		{/if}
		{label}
	</Button>
	<ButtonGroupSeparator />
	<DropdownMenu bind:open>
		<DropdownMenuTrigger>
			{#snippet child({ props })}
				<Button
					{...props}
					size={size === 'sm' ? 'icon-sm' : 'icon'}
					aria-label="Choose architecture"
				>
					<ChevronDownIcon aria-hidden="true" />
				</Button>
			{/snippet}
		</DropdownMenuTrigger>

		<DropdownMenuContent align="end" class="w-52">
			<DropdownMenuGroup>
				<DropdownMenuGroupHeading>Architecture</DropdownMenuGroupHeading>
				{#if isLoading}
					<div class="flex h-12 items-center justify-center">
						<Spinner class="stroke-primary size-4" />
					</div>
				{:else}
					<DropdownMenuRadioGroup value={selected} onValueChange={choose}>
						<DropdownMenuRadioItem value={DEFAULT_VALUE}>Default</DropdownMenuRadioItem>
						{#each architectures ?? [] as architecture (architecture)}
							<DropdownMenuRadioItem value={architecture}>{architecture}</DropdownMenuRadioItem>
						{/each}
					</DropdownMenuRadioGroup>
				{/if}
			</DropdownMenuGroup>
		</DropdownMenuContent>
	</DropdownMenu>
</ButtonGroup>
