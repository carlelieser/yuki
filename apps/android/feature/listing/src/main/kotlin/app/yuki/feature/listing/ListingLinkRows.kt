package app.yuki.feature.listing

import app.yuki.core.model.ListingDetail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ListingLinkRow(
    val label: String,
    val supporting: String,
    val url: String,
)

private const val LICENSE_LABEL = "License"
private const val LICENSE_BASE_URL = "https://choosealicense.com/licenses/"

fun licenseUrl(license: String): String =
    LICENSE_BASE_URL + license.trim().lowercase().replace(' ', '-') + "/"

fun linkRows(detail: ListingDetail): List<ListingLinkRow> = buildList {
    add(
        ListingLinkRow(
            label = "Repository",
            supporting = detail.links.repositoryUrl,
            url = detail.links.repositoryUrl,
        ),
    )
    add(
        ListingLinkRow(
            label = "Author",
            supporting = detail.summary.author,
            url = detail.links.authorUrl,
        ),
    )

    val homepage = detail.links.homepageUrl
    if (homepage != null) {
        add(ListingLinkRow(label = "Homepage", supporting = homepage, url = homepage))
    }

    val license = detail.license
    if (license != null) {
        add(ListingLinkRow(label = LICENSE_LABEL, supporting = license, url = licenseUrl(license)))
    }
}

private val publishedFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())

fun formatPublished(publishedAt: Instant?): String =
    publishedAt?.let(publishedFormatter::format) ?: "Unreleased"
