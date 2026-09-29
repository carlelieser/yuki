package app.yuki.core.model

data class CatalogPackage(
    val packageName: String,
    val githubRepoId: Long,
    val slug: String,
    val title: String,
    val iconUrl: String?,
    val identities: List<SigningIdentity> = emptyList(),
)

data class SigningIdentity(
    val signers: Set<String>,
    val lineage: Set<String> = emptySet(),
)
