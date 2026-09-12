# Yuki

Monorepo running on Bun, with SvelteKit on the frontend and Drizzle ORM over
Postgres for persistence.

## Layout

| Path              | Package         | Purpose                                      |
| ----------------- | --------------- | -------------------------------------------- |
| `apps/web`        | `@yuki/web`     | SvelteKit application (Vite, adapter-node)   |
| `apps/scraper`    | `@yuki/scraper` | GitHub discovery and nightly listing refresh |
| `packages/db`     | `@yuki/db`      | Drizzle schema, client, and migrations       |
| `packages/github` | `@yuki/github`  | GitHub API client and release asset mapping  |
| `packages/ui`     | `@yuki/ui`      | shadcn-svelte components and theme tokens    |

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
connectivity to Postgres, returning 503 with the reason when it is unreachable.

Postgres is published on port `55432` so it does not collide with a server
already running on the host default.

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
| `bun run scrape`      | Refresh listings from GitHub                 |
| `bun run listings`    | Review, publish, and unpublish listings      |
| `bun run backfill`    | One-off repair of stored confidence          |

## Scraper

`apps/scraper` discovers Shizuku-based Android projects on GitHub and keeps
existing listings current. It needs `GITHUB_TOKEN` in `.env` (a classic or
fine-grained token with public read access is enough).

```sh
bun run scrape              # nightly refresh of known listings
bun run scrape --discover   # weekly, searches for repositories added since the last run
bun run scrape --seed       # one-off, exhausts the search space
```

The three modes answer different questions. Refresh asks what changed in what we
already have. Discovery asks what appeared since the last successful run. Seed
asks what exists at all, and is meant to be run once rather than on a schedule.

Discovery only counts repositories that are not already indexed, so a run spends
its budget on new findings instead of confirming known ones. Search is bounded to
repositories created within a date range: discovery starts at the last successful
run's finish time, seed starts at 2008.

GitHub pages search results to 1000 per query, so a range reporting more than that
cannot be walked. When that happens the range is halved and each half is searched
separately, repeating until every partition fits. Completed partitions are recorded
in `scrape_partitions`, so an interrupted seed resumes instead of restarting, and a
weekly run skips ground it has already covered.

Only repository search accepts a date range; code search has no `created:`
qualifier and silently returns zero results if given one. A code partition
therefore covers all of time and cannot be narrowed, which means recording one as
permanently complete would retire that query forever — after a seed, discovery
would issue no code searches at all and report nothing new. Code partitions
instead expire after 30 days, long enough that a seed spanning several dispatches
still resumes rather than rewalking itself.

Code search is capped at 10 requests per minute, which is why seed takes hours and
runs by hand. The nightly refresh instead issues conditional requests against the
5000/hour core quota, where `304 Not Modified` responses do not count against the
limit. Each listing tracks a separate ETag for the repository, its releases, its
readme, and its tree, so a listing is only skipped when all four are unchanged. A
repository's metadata does not change when a maintainer publishes a release, which
is why releases are checked independently.

A repository is only indexed when its tree holds an `AndroidManifest.xml` or a
Gradle build file. `topic:shizuku` and a readme mention are self-assigned labels,
so on their own they describe an interest in Shizuku rather than an app that uses
it — an awesome-list, a shell script collection, and a shader pack all carry the
topic. Those two signals now need that structural proof before a listing exists.
The check reads the tree the refresh already fetches for the icon, so it costs no
extra requests, and it withholds a verdict when the tree came back `304` or
truncated rather than guessing.

A newly discovered listing goes live when it ships a downloadable APK _and_ its
evidence is better than weak. Self-declared evidence alone leaves it waiting in
the review queue:

```sh
bun run listings pending              # candidates, with the evidence behind each
bun run listings publish <slug>...    # put them on the storefront
bun run listings unpublish <slug>...  # take them back off
bun run listings published            # what is live right now
```

Publishing happens on insert only, never on refresh. A nightly run therefore
cannot resurrect something you unpublished, so taking a listing down sticks.

Confidence is derived from the evidence stored against a listing, recomputed
after each refresh writes its evidence. A refresh that rediscovers nothing new
therefore leaves a listing's grade alone instead of flattening it to `weak`.

`bun run backfill` repairs listings stored before that was true, and unpublishes
a hand-audited set of entries that carry the topic but build no Android app. It
prints what it would change and writes nothing until passed `--apply`:

```sh
bun run backfill            # report only
bun run backfill --apply    # write the changes
```

Each run records its counters in `scrape_runs` — including `request_count`
versus `not_modified_count`, which is how you tell the conditional requests are
working. `main.ts` exits nonzero on failure, so a failed scheduled run shows up
as a failed workflow.

`.github/workflows/scrape.yml` runs the refresh nightly and discovery weekly, and
either can be started by hand from the Actions tab. Seed lives in its own
`seed.yml`, dispatch only, and shares the `scrape` concurrency group so it cannot
overlap a scheduled run. Both jobs need two
repository secrets:

| Secret                 | Purpose                                             |
| ---------------------- | --------------------------------------------------- |
| `DATABASE_URL`         | Postgres connection string                          |
| `SCRAPER_GITHUB_TOKEN` | PAT with public read access, used as `GITHUB_TOKEN` |

The automatic `secrets.GITHUB_TOKEN` cannot be used: it is scoped to this
repository, and discovery searches all of GitHub.

## Database

Schema lives in `packages/db/src/schema`. After changing it, run
`bun run db:generate` to emit SQL into `packages/db/drizzle`, then
`bun run db:migrate` to apply it. Generated migrations are committed.
