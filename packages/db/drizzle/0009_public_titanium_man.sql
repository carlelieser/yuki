CREATE TABLE "scrape_searched_partitions" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"partition" text NOT NULL,
	"completed_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
DROP TABLE "scrape_partitions" CASCADE;--> statement-breakpoint
CREATE UNIQUE INDEX "scrape_searched_partitions_key" ON "scrape_searched_partitions" USING btree ("partition");