<script lang="ts">
	import { cn } from '@yuki/ui';
	import SearchIcon from '@lucide/svelte/icons/search';
	import { Button } from '@yuki/ui/components/button';
	import { Shortcut } from '@yuki/ui/components/dropdown-menu';

	let { class: className, onopen }: { class?: string; onopen: () => void } = $props();
</script>

<Button
	variant="ghost"
	class={cn(
		'sm:bg-secondary sm:text-secondary-foreground sm:hover:bg-[color-mix(in_oklch,var(--secondary),var(--foreground)_5%)] sm:hover:text-secondary-foreground dark:sm:hover:bg-[color-mix(in_oklch,var(--secondary),var(--foreground)_5%)]',
		className
	)}
	aria-label="Search"
	onclick={onopen}
>
	<SearchIcon class="size-4 shrink-0" aria-hidden="true" />
	<Shortcut class="hidden lg:block">⌘K</Shortcut>
</Button>
