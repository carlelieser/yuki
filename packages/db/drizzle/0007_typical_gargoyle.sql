ALTER TABLE "listing_installs" DISABLE ROW LEVEL SECURITY;--> statement-breakpoint
DROP TABLE "listing_installs" CASCADE;--> statement-breakpoint
DROP INDEX "listings_package_name_idx";--> statement-breakpoint
ALTER TABLE "listings" DROP COLUMN "package_name";