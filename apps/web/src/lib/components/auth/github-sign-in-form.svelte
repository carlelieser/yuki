<script lang="ts">
	import { resolve } from '$app/paths';

	type Props = { id: string; redirectTo?: string };

	let { id, redirectTo = '/' }: Props = $props();
</script>

<form {id} method="POST" action={resolve('/auth/github')} hidden>
	<input type="hidden" name="redirectTo" value={redirectTo} />
</form>
