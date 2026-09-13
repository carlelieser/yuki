<script lang="ts" module>
	import Button, { type ButtonProps } from '../button/button.svelte';
	import { cn } from '../../../cn.ts';

	export type LinkProps = Omit<ButtonProps, 'variant'>;
</script>

<script lang="ts">
	let { class: className, children, ...restProps }: LinkProps = $props();
</script>

<Button
	variant="link"
	class={cn('inline-block h-auto px-0 text-[length:inherit]', className)}
	{...restProps}
>
	{@render children?.()}
</Button>
