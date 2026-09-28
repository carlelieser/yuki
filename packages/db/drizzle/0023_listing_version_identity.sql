ALTER TABLE "listing_versions" ADD COLUMN "package_name" text;--> statement-breakpoint
ALTER TABLE "listing_versions" ADD COLUMN "signer_digests" text[];--> statement-breakpoint
ALTER TABLE "listing_versions" ADD COLUMN "lineage_digests" text[];--> statement-breakpoint
ALTER TABLE "listing_versions" ADD COLUMN "identity_read_at" timestamp with time zone;--> statement-breakpoint
CREATE INDEX "listing_versions_package_name_idx" ON "listing_versions" USING btree ("package_name");--> statement-breakpoint
CREATE INDEX "listing_versions_identity_pending_idx" ON "listing_versions" USING btree ("published_at") WHERE "listing_versions"."identity_read_at" is null and "listing_versions"."download_url" is not null;