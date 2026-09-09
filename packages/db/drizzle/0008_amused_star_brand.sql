CREATE TABLE "scrape_partitions" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"query" text NOT NULL,
	"since" timestamp with time zone NOT NULL,
	"until" timestamp with time zone NOT NULL,
	"completed_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX "scrape_partitions_key" ON "scrape_partitions" USING btree ("query","since","until");