CREATE TABLE "listing_version_assets" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"version_id" uuid NOT NULL,
	"name" text NOT NULL,
	"download_url" text NOT NULL,
	"size" integer NOT NULL,
	"download_count" integer DEFAULT 0 NOT NULL,
	"package_name" text,
	"signer_digests" text[],
	"lineage_digests" text[],
	"is_foreign" boolean DEFAULT false NOT NULL,
	"identity_read_at" timestamp with time zone,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
DROP INDEX "listing_versions_package_name_idx";--> statement-breakpoint
DROP INDEX "listing_versions_identity_pending_idx";--> statement-breakpoint
ALTER TABLE "listing_versions" ADD COLUMN "is_ignored" boolean DEFAULT false NOT NULL;--> statement-breakpoint
ALTER TABLE "listing_version_assets" ADD CONSTRAINT "listing_version_assets_version_id_listing_versions_id_fk" FOREIGN KEY ("version_id") REFERENCES "public"."listing_versions"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE UNIQUE INDEX "listing_version_assets_version_name_key" ON "listing_version_assets" USING btree ("version_id","name");--> statement-breakpoint
CREATE INDEX "listing_version_assets_package_name_idx" ON "listing_version_assets" USING btree ("package_name");--> statement-breakpoint
CREATE INDEX "listing_version_assets_identity_pending_idx" ON "listing_version_assets" USING btree ("version_id") WHERE "listing_version_assets"."identity_read_at" is null;--> statement-breakpoint
ALTER TABLE "listing_versions" DROP COLUMN "package_name";--> statement-breakpoint
ALTER TABLE "listing_versions" DROP COLUMN "signer_digests";--> statement-breakpoint
ALTER TABLE "listing_versions" DROP COLUMN "lineage_digests";--> statement-breakpoint
ALTER TABLE "listing_versions" DROP COLUMN "identity_read_at";--> statement-breakpoint
DELETE FROM "scrape_sources" WHERE "resource" LIKE 'repos/%/releases';