<script lang="ts">
	import { Button } from '@yuki/ui';
</script>

<main class="mx-auto flex min-h-screen max-w-2xl flex-col items-center justify-center gap-6 p-8">
	<h1 class="text-3xl font-semibold tracking-tight">Yuki</h1>
	<p class="text-muted-foreground text-center text-sm">SvelteKit, Drizzle, and Redis on Bun.</p>
	<Button href="/api/health" variant="outline">Check service health</Button>
</main>
