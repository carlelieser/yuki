package app.yuki.core.database

import androidx.room.Entity
import app.yuki.core.model.CatalogPackage
import app.yuki.core.model.SigningIdentity

@Entity(tableName = "package_index", primaryKeys = ["packageName", "githubRepoId"])
data class PackageIndexEntity(
    val packageName: String,
    val githubRepoId: Long,
    val slug: String,
    val title: String,
    val iconUrl: String?,
    val identities: String,
)

private const val IDENTITY_SEPARATOR = ";"
private const val PART_SEPARATOR = "|"
private const val DIGEST_SEPARATOR = ","

fun PackageIndexEntity.toCatalogPackage(): CatalogPackage = CatalogPackage(
    packageName = packageName,
    githubRepoId = githubRepoId,
    slug = slug,
    title = title,
    iconUrl = iconUrl,
    identities = decodeIdentities(identities),
)

fun CatalogPackage.toEntity(): PackageIndexEntity = PackageIndexEntity(
    packageName = packageName,
    githubRepoId = githubRepoId,
    slug = slug,
    title = title,
    iconUrl = iconUrl,
    identities = encodeIdentities(identities),
)

internal fun encodeIdentities(identities: List<SigningIdentity>): String =
    identities.joinToString(IDENTITY_SEPARATOR) { identity ->
        identity.signers.sorted().joinToString(DIGEST_SEPARATOR) +
            PART_SEPARATOR +
            identity.lineage.sorted().joinToString(DIGEST_SEPARATOR)
    }

internal fun decodeIdentities(encoded: String): List<SigningIdentity> =
    encoded.split(IDENTITY_SEPARATOR)
        .filter(String::isNotEmpty)
        .mapNotNull { identity ->
            val signers = digests(identity.substringBefore(PART_SEPARATOR))
            if (signers.isEmpty()) return@mapNotNull null

            SigningIdentity(signers, digests(identity.substringAfter(PART_SEPARATOR, "")))
        }

private fun digests(encoded: String): Set<String> =
    encoded.split(DIGEST_SEPARATOR).filter(String::isNotEmpty).toSet()
