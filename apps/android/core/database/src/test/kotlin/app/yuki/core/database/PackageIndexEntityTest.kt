package app.yuki.core.database

import app.yuki.core.model.CatalogPackage
import app.yuki.core.model.SigningIdentity
import org.junit.Assert.assertEquals
import org.junit.Test

class PackageIndexEntityTest {
    @Test
    fun keepsEverySigningIdentityThroughTheCache() {
        val entry = CatalogPackage(
            packageName = "com.google.ar.core",
            githubRepoId = 9L,
            slug = "neko",
            title = "Neko XRManager",
            iconUrl = null,
            identities = listOf(
                SigningIdentity(signers = setOf("6e91")),
                SigningIdentity(signers = setOf("0002"), lineage = setOf("0001", "0000")),
                SigningIdentity(signers = setOf("aa", "bb")),
            ),
        )

        assertEquals(entry, entry.toEntity().toCatalogPackage())
    }

    @Test
    fun readsAnEntryWithoutIdentitiesAsUnsigned() {
        assertEquals(emptyList<SigningIdentity>(), decodeIdentities(""))
    }

    @Test
    fun dropsAnIdentityThatNamesNoSigner() {
        assertEquals(
            listOf(SigningIdentity(signers = setOf("aa"))),
            decodeIdentities("|0001;aa|"),
        )
    }
}
