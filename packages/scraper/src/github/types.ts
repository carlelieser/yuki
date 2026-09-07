export type GithubResponse<Body> =
	{ isModified: true; body: Body; etag: string | null } | { isModified: false };

export type GithubRepository = {
	id: number;
	name: string;
	full_name: string;
	description: string | null;
	html_url: string;
	homepage: string | null;
	default_branch: string;
	stargazers_count: number;
	fork: boolean;
	archived: boolean;
	pushed_at: string | null;
	topics?: string[];
	license: { spdx_id: string | null } | null;
	owner: { login: string; html_url: string };
};

export type GithubCodeSearchItem = {
	name: string;
	path: string;
	repository: GithubRepository;
};

export type GithubCodeSearchResult = {
	total_count: number;
	incomplete_results: boolean;
	items: GithubCodeSearchItem[];
};

export type GithubRepoSearchResult = {
	total_count: number;
	incomplete_results: boolean;
	items: GithubRepository[];
};

export type GithubReleaseAsset = {
	name: string;
	browser_download_url: string;
	size: number;
	download_count: number;
	content_type: string;
};

export type GithubRelease = {
	tag_name: string;
	name: string | null;
	body: string | null;
	draft: boolean;
	prerelease: boolean;
	published_at: string | null;
	assets: GithubReleaseAsset[];
};

export type GithubTreeEntry = {
	path: string;
	type: string;
	size?: number;
};

export type GithubTree = {
	tree: GithubTreeEntry[];
	truncated: boolean;
};

export type GithubReadme = {
	content: string;
	encoding: string;
};
