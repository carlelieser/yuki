DROP INDEX "listings_published_created_idx";--> statement-breakpoint
ALTER TABLE "listings" ADD COLUMN "published_at" timestamp with time zone;--> statement-breakpoint
ALTER TABLE "listings" ADD COLUMN "latest_release_at" timestamp with time zone;--> statement-breakpoint
CREATE INDEX "listings_published_published_at_idx" ON "listings" USING btree ("is_published","published_at");--> statement-breakpoint
CREATE INDEX "listings_published_release_idx" ON "listings" USING btree ("is_published","latest_release_at");--> statement-breakpoint
UPDATE "listings" SET "published_at" = "created_at" WHERE "is_published";--> statement-breakpoint
UPDATE "listings" SET "latest_release_at" = "releases"."latest"
FROM (
	SELECT "listing_id", max("published_at") AS "latest"
	FROM "listing_versions"
	WHERE NOT "is_prerelease" AND "download_url" IS NOT NULL
	GROUP BY "listing_id"
) AS "releases"
WHERE "releases"."listing_id" = "listings"."id";
