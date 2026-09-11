package app.yuki.feature.listing

import app.yuki.core.model.ListingDetail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ListingLinkKind(val label: String) {
    Repository("Repository"),
    Author("Author"),
    Homepage("Homepage"),
    License("License"),
}

data class ListingLinkRow(
    val kind: ListingLinkKind,
    val supporting: String,
    val url: String,
) {
    val label: String get() = kind.label
}

private const val LICENSE_BASE_URL = "https://choosealicense.com/licenses/"

fun licenseUrl(license: String): String =
    LICENSE_BASE_URL + license.trim().lowercase().replace(' ', '-') + "/"

fun linkRows(detail: ListingDetail): List<ListingLinkRow> = buildList {
    add(
        ListingLinkRow(
            kind = ListingLinkKind.Repository,
            supporting = detail.links.repositoryUrl,
            url = detail.links.repositoryUrl,
        ),
    )
    add(
        ListingLinkRow(
            kind = ListingLinkKind.Author,
            supporting = detail.summary.author,
            url = detail.links.authorUrl,
        ),
    )

    val homepage = detail.links.homepageUrl
    if (homepage != null) {
        add(
            ListingLinkRow(
                kind = ListingLinkKind.Homepage,
                supporting = homepage,
                url = homepage,
            ),
        )
    }

    val license = detail.license
    if (license != null) {
        add(
            ListingLinkRow(
                kind = ListingLinkKind.License,
                supporting = license,
                url = licenseUrl(license),
            ),
        )
    }
}

private val publishedFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())

fun formatPublished(publishedAt: Instant?): String =
    publishedAt?.let(publishedFormatter::format) ?: "Unreleased"
