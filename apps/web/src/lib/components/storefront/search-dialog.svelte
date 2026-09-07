<script lang="ts">
	import { resolve } from '$app/paths';
	import {
		CommandDialog,
		CommandEmpty,
		CommandGroup,
		CommandInput,
		CommandItem,
		CommandLinkItem,
		CommandList,
		CommandLoading
	} from '@yuki/ui';
	import SearchIcon from '@lucide/svelte/icons/search';
	import ClockIcon from '@lucide/svelte/icons/clock';
	import XIcon from '@lucide/svelte/icons/x';
	import { Button } from '@yuki/ui/components/button';
	import { normalizeSearchQuery } from '$lib/search-query.ts';
	import {
		addRecentSearch,
		parseRecentSearches,
		RECENT_SEARCHES_KEY,
		removeRecentSearch
	} from '$lib/recent-searches.ts';
	import type { ListingSummary } from '$lib/server/listings.ts';

	const DEBOUNCE_MS = 150;

	let { open = $bindable(false) }: { open?: boolean } = $props();

	let query = $state('');
	let results = $state<ListingSummary[]>([]);
	let isLoading = $state(false);
	let recentSearches = $state<string[]>([]);

	let activeRequest: AbortController | null = null;

	const term = $derived(normalizeSearchQuery(query));
	const allResultsHref = $derived(`${resolve('/(app)/search')}?q=${encodeURIComponent(term)}`);

	async function fetchResults(searchTerm: string): Promise<void> {
		activeRequest?.abort();
		const request = new AbortController();
		activeRequest = request;
		isLoading = true;

		try {
			const response = await fetch(`/api/search?q=${encodeURIComponent(searchTerm)}`, {
				signal: request.signal
			});
			if (activeRequest !== request) return;
			if (!response.ok) {
				results = [];
				throw new Error(`search request for "${searchTerm}" failed with status ${response.status}`);
			}

			const payload = (await response.json()) as { results: ListingSummary[] };
			if (activeRequest !== request) return;
			results = payload.results;
		} catch (cause) {
			if (request.signal.aborted) return;
			results = [];
			throw new Error(`search request for "${searchTerm}" failed`, { cause });
		} finally {
			if (activeRequest === request) {
				isLoading = false;
				activeRequest = null;
			}
		}
	}

	function readStoredSearches(): string[] {
		try {
			return parseRecentSearches(localStorage.getItem(RECENT_SEARCHES_KEY));
		} catch {
			return [];
		}
	}

	function storeSearches(updated: string[]): void {
		recentSearches = updated;
		try {
			localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(updated));
		} catch {
			return;
		}
	}

	function rememberSearch(searched: string): void {
		storeSearches(addRecentSearch(recentSearches, searched));
	}

	function forgetSearch(searched: string): void {
		storeSearches(removeRecentSearch(recentSearches, searched));
	}

	function forgetAllSearches(): void {
		storeSearches([]);
	}

	$effect(() => {
		if (open) {
			recentSearches = readStoredSearches();
			return;
		}
		query = '';
	});

	$effect(() => {
		if (term === '') {
			activeRequest?.abort();
			activeRequest = null;
			results = [];
			isLoading = false;
			return;
		}

		const timer = setTimeout(() => void fetchResults(term), DEBOUNCE_MS);
		return () => clearTimeout(timer);
	});
</script>

<CommandDialog bind:open shouldFilter={false} title="Search apps" class="min-h-48">
	<CommandInput placeholder="Search apps" bind:value={query} />
	<CommandList class="p-1">
		{#if term === '' && recentSearches.length > 0}
			<div class="flex items-center justify-between gap-2 px-2 pt-2">
				<span class="text-xs font-medium text-muted-foreground">Recent</span>
				<Button variant="ghost" size="xs" onclick={forgetAllSearches}>Clear</Button>
			</div>
			<CommandGroup>
				{#each recentSearches as recent (recent)}
					<CommandItem value="recent:{recent}" onSelect={() => (query = recent)}>
						<ClockIcon />
						<span class="flex-1 truncate">{recent}</span>
						<Button
							variant="ghost"
							size="icon-xs"
							class="-me-6 shrink-0"
							aria-label="Remove {recent} from recent searches"
							onclick={(event) => {
								event.stopPropagation();
								forgetSearch(recent);
							}}
						>
							<XIcon />
						</Button>
					</CommandItem>
				{/each}
			</CommandGroup>
		{/if}

		{#if term !== ''}
			<CommandLinkItem
				value="see-all-results"
				href={allResultsHref}
				onSelect={() => {
					rememberSearch(term);
					open = false;
				}}
			>
				<span class="text-muted-foreground">See all results for &ldquo;{term}&rdquo;</span>
			</CommandLinkItem>
		{/if}

		{#if isLoading}
			<CommandLoading>Searching&hellip;</CommandLoading>
		{/if}

		{#each results as entry (entry.id)}
			<CommandLinkItem
				value={entry.id}
				href={resolve('/(app)/listings/[slug]', { slug: entry.slug })}
				onSelect={() => {
					rememberSearch(term);
					open = false;
				}}
			>
				<SearchIcon />
				<span class="truncate">{entry.title}</span>
				<span class="ms-auto shrink-0 text-xs text-muted-foreground">{entry.author}</span>
			</CommandLinkItem>
		{/each}

		{#if !isLoading && term !== '' && results.length === 0}
			<CommandEmpty>No apps found.</CommandEmpty>
		{/if}
	</CommandList>
</CommandDialog>
