FROM oven/bun:1.4-alpine AS base
WORKDIR /app

FROM base AS deps
COPY package.json bun.lock ./
COPY apps/web/package.json apps/web/
COPY packages/auth/package.json packages/auth/
COPY packages/db/package.json packages/db/
COPY packages/ui/package.json packages/ui/
COPY packages/scraper/package.json packages/scraper/
RUN bun install --frozen-lockfile

FROM node:24-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules node_modules
COPY --from=deps /app/apps/web/node_modules apps/web/node_modules
COPY --from=deps /app/packages packages
COPY . .
WORKDIR /app/apps/web
RUN node node_modules/vite/bin/vite.js build
WORKDIR /app

FROM node:24-alpine AS release
WORKDIR /app
ENV NODE_ENV=production
COPY --from=deps /app/node_modules node_modules
COPY --from=deps /app/apps/web/node_modules apps/web/node_modules
COPY --from=build /app/apps/web/build apps/web/build
COPY --from=build /app/apps/web/package.json apps/web/
COPY --from=build /app/packages packages
COPY --from=build /app/package.json ./
ENV PORT=3000
EXPOSE 3000
CMD ["node", "apps/web/build/index.js"]
