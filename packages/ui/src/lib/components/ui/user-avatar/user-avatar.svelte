<script lang="ts">
	import { cn } from '../../../cn.ts';
	import Avatar from '../avatar/avatar.svelte';
	import AvatarFallback from '../avatar/avatar-fallback.svelte';
	import AvatarImage from '../avatar/avatar-image.svelte';

	let {
		initials,
		image = null,
		class: className
	}: {
		initials: string;
		image?: string | null;
		class?: string;
	} = $props();
</script>

<Avatar class={cn('size-9 shrink-0', className)}>
	{#if image}
		<AvatarImage src={image} alt="" />
	{/if}
	<AvatarFallback>{initials}</AvatarFallback>
</Avatar>
