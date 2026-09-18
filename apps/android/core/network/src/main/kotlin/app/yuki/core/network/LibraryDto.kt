package app.yuki.core.network

import app.yuki.core.model.LibraryEntry
import kotlinx.serialization.Serializable

@Serializable
internal data class LibraryEntryDto(
    val githubRepoId: Long,
    val slug: String,
    val title: String,
    val packageName: String? = null,
    val iconUrl: String? = null,
    val versionTag: String? = null,
)

@Serializable
internal data class LibraryPageDto(
    val results: List<LibraryEntryDto> = emptyList(),
)

@Serializable
internal data class LibraryRecordDto(
    val slug: String,
    val versionTag: String?,
)

internal fun LibraryEntryDto.toDomain(): LibraryEntry = LibraryEntry(
    githubRepoId = githubRepoId,
    packageName = packageName,
    slug = slug,
    title = title,
    iconUrl = iconUrl,
    versionTag = versionTag.orEmpty(),
)
