import { decideRetry } from './backoff.ts';
import type {
	GithubCodeSearchResult,
	GithubRelease,
	GithubRepoSearchResult,
	GithubRepository,
	GithubResponse,
	GithubTree
} from './types.ts';

const API_ORIGIN = 'https://api.github.com';
const USER_AGENT = 'yuki-scraper';

export type FetchImpl = (input: string, init?: RequestInit) => Promise<Response>;

export type GithubClient = ReturnType<typeof createGithubClient>;

export type RequestOptions = {
	etag?: string | null;
	accept?: string;
	asText?: boolean;
};

export class GithubSkip extends Error {}

export type ClientStats = {
	requestCount: number;
	notModifiedCount: number;
	pacedWaitMs: number;
};

function sleep(ms: number): Promise<void> {
	return new Promise((resolve) => setTimeout(resolve, ms));
}

export function createGithubClient(
	token: string,
	fetchImpl: FetchImpl = fetch,
	wait: (ms: number) => Promise<void> = sleep,
	maxAttempts?: number
) {
	const stats: ClientStats = { requestCount: 0, notModifiedCount: 0, pacedWaitMs: 0 };

	async function request<Body>(
		path: string,
		{ etag = null, accept = 'application/vnd.github+json', asText = false }: RequestOptions = {}
	): Promise<GithubResponse<Body>> {
		const url = path.startsWith('http') ? path : `${API_ORIGIN}${path}`;
		const isCodeSearch = url.includes('/search/code');

		let attempt = 1;

		for (;;) {
			const headers: Record<string, string> = {
				accept,
				authorization: `Bearer ${token}`,
				'user-agent': USER_AGENT,
				'x-github-api-version': '2022-11-28'
			};
			if (etag !== null) {
				headers['if-none-match'] = etag;
			}

			stats.requestCount += 1;
			const response = await fetchImpl(url, { headers });

			if (response.status === 304) {
				stats.notModifiedCount += 1;
				return { isModified: false };
			}

			const decision = decideRetry({
				status: response.status,
				headers: response.headers,
				attempt,
				maxAttempts,
				isCodeSearch,
				resource: path
			});

			if (decision.kind === 'succeed') {
				return {
					isModified: true,
					body: (asText ? await response.text() : await response.json()) as Body,
					etag: response.headers.get('etag')
				};
			}

			if (decision.kind === 'pace') {
				stats.pacedWaitMs += decision.waitMs;
				await wait(decision.waitMs);
				continue;
			}

			if (decision.kind === 'retry') {
				attempt += 1;
				await wait(decision.waitMs);
				continue;
			}

			if (decision.kind === 'skip') {
				throw new GithubSkip(decision.reason);
			}

			throw new Error(decision.reason);
		}
	}

	return {
		stats,

		request,

		searchCode(q: string, page: number, perPage: number) {
			const params = new URLSearchParams({
				q,
				per_page: String(perPage),
				page: String(page)
			});
			return request<GithubCodeSearchResult>(`/search/code?${params.toString()}`);
		},

		searchRepositories(q: string, page: number, perPage: number) {
			const params = new URLSearchParams({
				q,
				per_page: String(perPage),
				page: String(page)
			});
			return request<GithubRepoSearchResult>(`/search/repositories?${params.toString()}`);
		},

		getRepository(owner: string, name: string, etag: string | null = null) {
			return request<GithubRepository>(`/repos/${owner}/${name}`, { etag });
		},

		getReleases(owner: string, name: string, etag: string | null = null) {
			return request<GithubRelease[]>(`/repos/${owner}/${name}/releases?per_page=100`, { etag });
		},

		getReleaseByTag(owner: string, name: string, tag: string) {
			return request<GithubRelease>(
				`/repos/${owner}/${name}/releases/tags/${encodeURIComponent(tag)}`
			);
		},

		getReadme(owner: string, name: string, etag: string | null = null) {
			return request<string>(`/repos/${owner}/${name}/readme`, {
				etag,
				accept: 'application/vnd.github.raw',
				asText: true
			});
		},

		getTree(owner: string, name: string, branch: string, etag: string | null = null) {
			return request<GithubTree>(`/repos/${owner}/${name}/git/trees/${branch}?recursive=1`, {
				etag
			});
		},

		getBlob(owner: string, name: string, sha: string) {
			return request<string>(`/repos/${owner}/${name}/git/blobs/${sha}`, {
				accept: 'application/vnd.github.raw',
				asText: true
			});
		},

		getRawFile(owner: string, name: string, path: string, ref: string) {
			const encoded = path
				.split('/')
				.map((segment) => encodeURIComponent(segment))
				.join('/');

			return request<string>(`/repos/${owner}/${name}/contents/${encoded}?ref=${ref}`, {
				accept: 'application/vnd.github.raw',
				asText: true
			});
		}
	};
}
