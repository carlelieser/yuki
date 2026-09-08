import { and, asc, desc, eq, sql, type SQL } from 'drizzle-orm';
import { schema, type Database } from '@yuki/db';
import { orderByFor, summaryColumns, type ListingSummary } from './listings.ts';
import {
	DEFAULT_SEARCH_SORTING,
	hasEnoughLengthForTrigram,
	isRelevanceSort,
	MAX_SEARCH_OFFSET,
	toPrefixTsQuery,
	type SearchSorting
} from '../search-query.ts';

export type SearchPage = {
	results: ListingSummary[];
	total: number;
	hasMore: boolean;
};

type SearchPageRequest = { limit: number; offset: number; sorting?: SearchSorting };

function fullTextMatch(tsQuery: SQL): SQL {
	return sql`${schema.listings.searchVector} @@ ${tsQuery}`;
}

function withFuzzyTitleMatch(normalized: string, ftsMatch: SQL): SQL {
	if (!hasEnoughLengthForTrigram(normalized)) return ftsMatch;
	return sql`(${ftsMatch} or ${normalized} <% ${schema.listings.title})`;
}

type SearchMatch = { condition: SQL; rank: SQL };

function matchFor(normalized: string, tsQuery: SQL): SearchMatch {
	return {
		condition: withFuzzyTitleMatch(normalized, fullTextMatch(tsQuery)),
		rank: sql`ts_rank_cd(${schema.listings.searchVector}, ${tsQuery})`
	};
}

function buildTypeaheadMatch(normalized: string): SearchMatch | null {
	const prefixQuery = toPrefixTsQuery(normalized);
	if (prefixQuery === '') return null;

	return matchFor(normalized, sql`to_tsquery('english', ${prefixQuery})`);
}

function buildFullSearchMatch(normalized: string): SearchMatch | null {
	if (normalized === '') return null;

	return matchFor(normalized, sql`websearch_to_tsquery('english', ${normalized})`);
}

function publishedAnd(condition: SQL): SQL {
	return and(eq(schema.listings.isPublished, true), condition) as SQL;
}

function summaryOf(row: ListingSummary & { total: number }): ListingSummary {
	return {
		id: row.id,
		githubRepoId: row.githubRepoId,
		slug: row.slug,
		title: row.title,
		author: row.author,
		description: row.description,
		iconUrl: row.iconUrl,
		bannerUrl: row.bannerUrl,
		stars: row.stars
	};
}

function relevanceOrderBy(rank: SQL): SQL[] {
	return [desc(rank), desc(schema.listings.stars), asc(schema.listings.id)];
}

function searchOrderBy(sorting: SearchSorting, rank: SQL): SQL[] {
	if (isRelevanceSort(sorting.sort)) return relevanceOrderBy(rank);
	return [...orderByFor(sorting.sort, sorting.order), desc(rank)];
}

export async function searchListingsTypeahead(
	db: Database,
	query: string,
	limit: number
): Promise<ListingSummary[]> {
	const match = buildTypeaheadMatch(query);
	if (match === null) return [];

	return db
		.select(summaryColumns)
		.from(schema.listings)
		.where(publishedAnd(match.condition))
		.orderBy(...relevanceOrderBy(match.rank))
		.limit(limit);
}

export async function searchListingsPage(
	db: Database,
	query: string,
	page: SearchPageRequest
): Promise<SearchPage> {
	const match = buildFullSearchMatch(query);
	if (match === null) return { results: [], total: 0, hasMore: false };

	const sorting = page.sorting ?? DEFAULT_SEARCH_SORTING;
	const offset = Math.min(page.offset, MAX_SEARCH_OFFSET);
	const rows = await db
		.select({ ...summaryColumns, total: sql<number>`count(*) over ()`.mapWith(Number) })
		.from(schema.listings)
		.where(publishedAnd(match.condition))
		.orderBy(...searchOrderBy(sorting, match.rank))
		.limit(page.limit)
		.offset(offset);

	const total = rows[0]?.total ?? 0;
	const results = rows.map((row) => summaryOf(row));

	return { results, total, hasMore: offset + results.length < total };
}
