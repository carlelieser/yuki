ALTER TABLE "listings" ADD COLUMN "package_name" text;--> statement-breakpoint
CREATE INDEX "listings_package_name_idx" ON "listings" USING btree ("package_name");