CREATE EXTENSION IF NOT EXISTS pg_trgm;--> statement-breakpoint
ALTER TABLE "listings" ADD COLUMN "search_vector" "tsvector" GENERATED ALWAYS AS (setweight(to_tsvector('english', "listings"."title"), 'A') || setweight(to_tsvector('english', "listings"."author"), 'B') || setweight(to_tsvector('english', coalesce("listings"."description", '')), 'C')) STORED;--> statement-breakpoint
CREATE INDEX "listings_search_vector_idx" ON "listings" USING gin ("search_vector");--> statement-breakpoint
CREATE INDEX "listings_search_title_trgm_idx" ON "listings" USING gin ("title" gin_trgm_ops);