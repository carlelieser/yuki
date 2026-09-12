ALTER TABLE "listings" drop column "search_vector";--> statement-breakpoint
ALTER TABLE "listings" ADD COLUMN "search_vector" "tsvector" GENERATED ALWAYS AS (setweight(to_tsvector('english', "listings"."title"), 'A') || setweight(to_tsvector('english', "listings"."name"), 'A') || setweight(to_tsvector('english', "listings"."author"), 'B') || setweight(to_tsvector('english', coalesce("listings"."description", '')), 'C')) STORED;--> statement-breakpoint
CREATE INDEX "listings_search_vector_idx" ON "listings" USING gin ("search_vector");
