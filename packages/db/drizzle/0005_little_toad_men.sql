CREATE TABLE "listing_downloads" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"listing_id" uuid NOT NULL,
	"user_id" text NOT NULL,
	"version_tag" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "listing_reviews" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"listing_id" uuid NOT NULL,
	"user_id" text NOT NULL,
	"rating" integer NOT NULL,
	"body" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "listing_reviews_rating_range" CHECK ("listing_reviews"."rating" between 1 and 5)
);
--> statement-breakpoint
ALTER TABLE "listing_downloads" ADD CONSTRAINT "listing_downloads_listing_id_listings_id_fk" FOREIGN KEY ("listing_id") REFERENCES "public"."listings"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "listing_downloads" ADD CONSTRAINT "listing_downloads_user_id_user_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."user"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "listing_reviews" ADD CONSTRAINT "listing_reviews_listing_id_listings_id_fk" FOREIGN KEY ("listing_id") REFERENCES "public"."listings"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "listing_reviews" ADD CONSTRAINT "listing_reviews_user_id_user_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."user"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE UNIQUE INDEX "listing_downloads_listing_user_key" ON "listing_downloads" USING btree ("listing_id","user_id");--> statement-breakpoint
CREATE INDEX "listing_downloads_user_idx" ON "listing_downloads" USING btree ("user_id");--> statement-breakpoint
CREATE UNIQUE INDEX "listing_reviews_listing_user_key" ON "listing_reviews" USING btree ("listing_id","user_id");--> statement-breakpoint
CREATE INDEX "listing_reviews_listing_created_idx" ON "listing_reviews" USING btree ("listing_id","created_at");