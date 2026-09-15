package app.yuki.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.yuki.core.model.CatalogPackage

@Entity(tableName = "package_index")
data class PackageIndexEntity(
    @PrimaryKey val packageName: String,
    val githubRepoId: Long,
    val slug: String,
    val title: String,
    val iconUrl: String?,
)

fun PackageIndexEntity.toCatalogPackage(): CatalogPackage = CatalogPackage(
    packageName = packageName,
    githubRepoId = githubRepoId,
    slug = slug,
    title = title,
    iconUrl = iconUrl,
)

fun CatalogPackage.toEntity(): PackageIndexEntity = PackageIndexEntity(
    packageName = packageName,
    githubRepoId = githubRepoId,
    slug = slug,
    title = title,
    iconUrl = iconUrl,
)
