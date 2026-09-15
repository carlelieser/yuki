package app.yuki.core.database

import app.yuki.core.model.CatalogPackage
import javax.inject.Inject
import javax.inject.Singleton

interface PackageIndexStore {
    suspend fun packages(): List<CatalogPackage>

    suspend fun isEmpty(): Boolean

    suspend fun replaceAll(packages: List<CatalogPackage>)
}

@Singleton
internal class RoomPackageIndexStore @Inject constructor(
    private val dao: PackageIndexDao,
) : PackageIndexStore {
    override suspend fun packages(): List<CatalogPackage> =
        dao.getAll().map(PackageIndexEntity::toCatalogPackage)

    override suspend fun isEmpty(): Boolean = dao.count() == 0

    override suspend fun replaceAll(packages: List<CatalogPackage>) {
        dao.replaceAll(packages.map(CatalogPackage::toEntity))
    }
}
