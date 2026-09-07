import { describe, expect, it } from 'vitest';
import {
	addRecentSearch,
	MAX_RECENT_SEARCHES,
	parseRecentSearches,
	removeRecentSearch
} from './recent-searches.ts';

describe('parseRecentSearches', () => {
	it('reads a stored list of terms', () => {
		expect(parseRecentSearches('["file explorer","logfox"]')).toEqual(['file explorer', 'logfox']);
	});

	it('returns an empty list when nothing is stored', () => {
		expect(parseRecentSearches(null)).toEqual([]);
	});

	it('returns an empty list for malformed or unexpected payloads', () => {
		expect(parseRecentSearches('not json')).toEqual([]);
		expect(parseRecentSearches('{"terms":[]}')).toEqual([]);
		expect(parseRecentSearches('"a string"')).toEqual([]);
	});

	it('drops entries that are not usable search terms', () => {
		expect(parseRecentSearches('["  ",42,null,"logfox"]')).toEqual(['logfox']);
	});

	it('normalizes stored terms', () => {
		expect(parseRecentSearches('["  file   explorer  "]')).toEqual(['file explorer']);
	});

	it('caps a stored list that grew too long', () => {
		const stored = JSON.stringify(['a1', 'b2', 'c3', 'd4', 'e5', 'f6', 'g7']);
		expect(parseRecentSearches(stored)).toHaveLength(MAX_RECENT_SEARCHES);
	});
});

describe('addRecentSearch', () => {
	it('puts the newest term first', () => {
		expect(addRecentSearch(['logfox'], 'butler')).toEqual(['butler', 'logfox']);
	});

	it('ignores blank terms', () => {
		expect(addRecentSearch(['logfox'], '   ')).toEqual(['logfox']);
		expect(addRecentSearch(['logfox'], '')).toEqual(['logfox']);
	});

	it('moves a repeated term back to the front instead of duplicating it', () => {
		expect(addRecentSearch(['butler', 'logfox'], 'logfox')).toEqual(['logfox', 'butler']);
	});

	it('treats terms differing only by case as the same search', () => {
		expect(addRecentSearch(['LogFox'], 'logfox')).toEqual(['logfox']);
	});

	it('keeps only the most recent searches', () => {
		const full = ['a1', 'b2', 'c3', 'd4', 'e5'];
		const updated = addRecentSearch(full, 'f6');
		expect(updated).toHaveLength(MAX_RECENT_SEARCHES);
		expect(updated[0]).toBe('f6');
		expect(updated).not.toContain('e5');
	});
});

describe('removeRecentSearch', () => {
	it('drops the named term and keeps the rest in order', () => {
		expect(removeRecentSearch(['butler', 'logfox', 'blocker'], 'logfox')).toEqual([
			'butler',
			'blocker'
		]);
	});

	it('removes a term regardless of how it was cased', () => {
		expect(removeRecentSearch(['LogFox'], 'logfox')).toEqual([]);
		expect(removeRecentSearch(['logfox'], 'LOGFOX')).toEqual([]);
	});

	it('matches the term the way it was stored', () => {
		expect(removeRecentSearch(['file explorer'], '  file   explorer  ')).toEqual([]);
	});

	it('leaves the list alone when the term is absent', () => {
		expect(removeRecentSearch(['butler'], 'logfox')).toEqual(['butler']);
	});

	it('ignores blank terms rather than clearing the list', () => {
		expect(removeRecentSearch(['butler'], '   ')).toEqual(['butler']);
		expect(removeRecentSearch(['butler'], '')).toEqual(['butler']);
	});
});
