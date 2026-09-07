# Yuki

Monorepo running on Bun, with SvelteKit on the frontend, Drizzle ORM over
Postgres for persistence, and Redis for caching.

## Layout

| Path             | Package       | Purpose                                    |
| ---------------- | ------------- | ------------------------------------------ |
| `apps/web`       | `@yuki/web`   | SvelteKit application (Vite, adapter-node) |
| `packages/db`    | `@yuki/db`    | Drizzle schema, client, and migrations     |
| `packages/redis` | `@yuki/redis` | Redis client factory                       |
| `packages/ui`    | `@yuki/ui`    | shadcn-svelte components and theme tokens  |

## Getting started

Requires Bun 1.4+ and Docker.

```sh
bun install
cp .env.example .env
docker compose up -d
bun run db:migrate
bun run dev
```

The app is served at http://localhost:5173. `GET /api/health` verifies
connectivity to Postgres and Redis, returning 503 with a per-dependency reason
when either is unreachable.

Postgres and Redis are published on ports `55432` and `56379` so they do not
collide with services already running on the host defaults.

## Scripts

| Command               | Description                                  |
| --------------------- | -------------------------------------------- |
| `bun run dev`         | Start the app in development mode            |
| `bun run build`       | Production build of every package            |
| `bun run check`       | Typecheck the whole workspace                |
| `bun run test`        | Run the test suite                           |
| `bun run lint`        | Prettier check plus ESLint                   |
| `bun run format`      | Apply Prettier formatting                    |
| `bun run db:generate` | Generate a migration from the Drizzle schema |
| `bun run db:migrate`  | Apply pending migrations                     |
| `bun run db:studio`   | Open Drizzle Studio                          |

## Database

Schema lives in `packages/db/src/schema`. After changing it, run
`bun run db:generate` to emit SQL into `packages/db/drizzle`, then
`bun run db:migrate` to apply it. Generated migrations are committed.
