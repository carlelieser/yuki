<script lang="ts">
	import { siGithub } from 'simple-icons';
	import type { SVGAttributes } from 'svelte/elements';

	let {
		size = 24,
		class: className,
		...restProps
	}: SVGAttributes<SVGElement> & { size?: number | string } = $props();
</script>

<svg
	xmlns="http://www.w3.org/2000/svg"
	viewBox="0 0 24 24"
	width={size}
	height={size}
	fill="currentColor"
	aria-hidden="true"
	class={className}
	{...restProps}
>
	<path d={siGithub.path} />
</svg>
