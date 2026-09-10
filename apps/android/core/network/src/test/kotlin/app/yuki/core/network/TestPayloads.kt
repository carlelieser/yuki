package app.yuki.core.network

internal const val SUMMARY_JSON = """
{
  "id": "6f1d0f3a-0000-4000-8000-000000000001",
  "githubRepoId": 1234567890,
  "slug": "example-app",
  "title": "Example App",
  "author": "octocat",
  "description": null,
  "iconUrl": null,
  "bannerUrl": null,
  "category": "developer_tools",
  "stars": 42
}
"""

internal const val UNCATEGORISED_SUMMARY_JSON = """
{
  "id": "6f1d0f3a-0000-4000-8000-000000000002",
  "githubRepoId": 1234567891,
  "slug": "uncategorised-app",
  "title": "Uncategorised App",
  "author": "octocat",
  "description": null,
  "iconUrl": null,
  "bannerUrl": null,
  "stars": 7
}
"""

internal const val UNKNOWN_CATEGORY_SUMMARY_JSON = """
{
  "id": "6f1d0f3a-0000-4000-8000-000000000003",
  "githubRepoId": 1234567892,
  "slug": "future-app",
  "title": "Future App",
  "author": "octocat",
  "description": null,
  "iconUrl": null,
  "bannerUrl": null,
  "category": "teleportation",
  "stars": 9
}
"""

internal const val UNCATEGORISED_PAGE_JSON =
    """{"results":[$UNCATEGORISED_SUMMARY_JSON],"hasMore":false}"""

internal const val UNKNOWN_CATEGORY_PAGE_JSON =
    """{"results":[$UNKNOWN_CATEGORY_SUMMARY_JSON],"hasMore":false}"""

internal const val BROWSE_PAGE_JSON = """{"results":[$SUMMARY_JSON],"hasMore":true}"""

internal const val FEATURED_PAGE_JSON = """{"results":[$SUMMARY_JSON],"hasMore":false}"""

internal const val SEARCH_RESULTS_JSON = """{"results":[$SUMMARY_JSON]}"""

internal const val EMPTY_PAGE_JSON = """{"results":[],"hasMore":false}"""

internal const val DETAIL_JSON = """
{
  "id": "6f1d0f3a-0000-4000-8000-000000000001",
  "githubRepoId": 1234567890,
  "slug": "example-app",
  "title": "Example App",
  "author": "octocat",
  "description": "A sample listing",
  "iconUrl": "https://cdn.test/icon.png",
  "bannerUrl": null,
  "category": "developer_tools",
  "stars": 42,
  "authorUrl": "https://github.com/octocat",
  "repositoryUrl": "https://github.com/octocat/example-app",
  "homepageUrl": null,
  "license": "MIT",
  "isArchived": false,
  "screenshots": [{ "url": "https://cdn.test/one.png", "alt": null }],
  "versions": [
    {
      "tag": "v1.2.0",
      "name": "1.2.0",
      "downloadUrl": "https://cdn.test/app-1.2.0.apk",
      "assetName": "app.apk",
      "isPrerelease": false,
      "publishedAt": "2026-01-02T03:04:05.000Z"
    },
    {
      "tag": "v1.1.0",
      "name": null,
      "downloadUrl": null,
      "assetName": null,
      "isPrerelease": true,
      "publishedAt": null
    }
  ]
}
"""

internal const val SECTIONS_JSON = """
{
  "sections": [
    { "category": "gaming", "results": [$SUMMARY_JSON] },
    { "category": "media", "results": [$UNCATEGORISED_SUMMARY_JSON] }
  ]
}
"""

internal const val UNKNOWN_SECTION_JSON = """
{
  "sections": [
    { "category": "teleportation", "results": [$SUMMARY_JSON] },
    { "category": "gaming", "results": [$SUMMARY_JSON] }
  ]
}
"""

internal const val EMPTY_SECTIONS_JSON = """{"sections":[]}"""
