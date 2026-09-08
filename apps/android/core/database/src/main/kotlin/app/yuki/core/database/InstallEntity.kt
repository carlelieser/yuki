package app.yuki.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.yuki.core.model.InstalledApp
import java.time.Instant

@Entity(tableName = "installs")
data class InstallEntity(
    @PrimaryKey val githubRepoId: Long,
    val packageName: String,
    val slug: String,
    val title: String,
    val iconUrl: String?,
    val versionTag: String,
    val versionCode: Long?,
    val installedAt: Instant,
)

fun InstallEntity.toInstalledApp(): InstalledApp = InstalledApp(
    githubRepoId = githubRepoId,
    packageName = packageName,
    slug = slug,
    title = title,
    iconUrl = iconUrl,
    versionTag = versionTag,
)
