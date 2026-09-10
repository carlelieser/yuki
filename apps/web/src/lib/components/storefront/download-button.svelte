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
		Skeleton,
		type ButtonSize
	} from '@yuki/ui';
	import type { Architecture } from '@yuki/github';
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

	let open = $state(false);
	let hasRequested = $state(false);
	let isLoading = $state(false);
	let architectures = $state<Architecture[]>([]);
	let selected = $state(DEFAULT_VALUE);

	const downloadPath = $derived(resolve('/(app)/listings/[slug]/download/[tag]', { slug, tag }));

	function urlFor(architecture: string): string {
		if (architecture === DEFAULT_VALUE) return downloadPath;
		return `${downloadPath}?arch=${encodeURIComponent(architecture)}`;
	}

	const href = $derived(urlFor(selected));

	const buttonLabel = $derived(selected === DEFAULT_VALUE ? label : `${label} · ${selected}`);

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
		{buttonLabel}
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
					<Skeleton class="mx-2 my-1.5 h-6 rounded-sm" />
				{:else}
					<DropdownMenuRadioGroup bind:value={selected}>
						<DropdownMenuRadioItem value={DEFAULT_VALUE}>Default</DropdownMenuRadioItem>
						{#each architectures as architecture (architecture)}
							<DropdownMenuRadioItem value={architecture}>{architecture}</DropdownMenuRadioItem>
						{/each}
					</DropdownMenuRadioGroup>
				{/if}
			</DropdownMenuGroup>
		</DropdownMenuContent>
	</DropdownMenu>
</ButtonGroup>
