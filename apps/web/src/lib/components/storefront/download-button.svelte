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

	const selected = $derived(selectedArchitecture(store.preference, architectures) ?? DEFAULT_VALUE);

	const downloadPath = $derived(resolve('/(app)/listings/[slug]/download/[tag]', { slug, tag }));

	function urlFor(architecture: string): string {
		if (architecture === DEFAULT_VALUE) return downloadPath;
		return `${downloadPath}?arch=${encodeURIComponent(architecture)}`;
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
	<Button {href} {size} data-sveltekit-preload-data="off" rel="nofollow">
		<DownloadIcon />
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
					<div class="flex h-8 items-center justify-center">
						<Spinner class="text-muted-foreground" />
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
