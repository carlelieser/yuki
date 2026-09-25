package app.yuki.feature.listing

import androidx.annotation.StringRes
import app.yuki.core.model.ListingDetail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class ListingLinkKind(@StringRes val label: Int) {
    Repository(R.string.listing_link_repository),
    Author(R.string.listing_link_author),
    Homepage(R.string.listing_link_homepage),
    License(R.string.listing_link_license),
}

data class ListingLinkRow(
    val kind: ListingLinkKind,
    val supporting: String,
    val url: String,
)

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

fun formatPublished(publishedAt: Instant, locale: Locale, zone: ZoneId): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(zone)
        .format(publishedAt)
