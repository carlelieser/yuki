CREATE TABLE "listing_installs" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"listing_id" uuid NOT NULL,
	"user_id" text NOT NULL,
	"package_name" text NOT NULL,
	"version_tag" text,
	"version_code" text,
	"installed_at" timestamp with time zone DEFAULT now() NOT NULL,
	"removed_at" timestamp with time zone,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "listings" ADD COLUMN "package_name" text;--> statement-breakpoint
ALTER TABLE "listing_installs" ADD CONSTRAINT "listing_installs_listing_id_listings_id_fk" FOREIGN KEY ("listing_id") REFERENCES "public"."listings"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "listing_installs" ADD CONSTRAINT "listing_installs_user_id_user_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."user"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE UNIQUE INDEX "listing_installs_listing_user_key" ON "listing_installs" USING btree ("listing_id","user_id");--> statement-breakpoint
CREATE INDEX "listing_installs_user_active_idx" ON "listing_installs" USING btree ("user_id","removed_at");--> statement-breakpoint
CREATE INDEX "listing_installs_package_idx" ON "listing_installs" USING btree ("package_name");--> statement-breakpoint
CREATE INDEX "listings_package_name_idx" ON "listings" USING btree ("package_name");