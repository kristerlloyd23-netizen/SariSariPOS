package com.sarisari.pos.data.repository

import androidx.room.withTransaction
import com.sarisari.pos.data.local.AppDatabase
import com.sarisari.pos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val db: AppDatabase) {

    private val dao = db.productDao()

    fun observeAll(): Flow<List<ProductEntity>> = dao.observeAll()

    fun observeLowStock(): Flow<List<ProductEntity>> = dao.observeLowStock()

    suspend fun getByBarcode(barcode: String): ProductEntity? = dao.getByBarcode(barcode.trim())

    /** Inserts when id == 0, otherwise updates. Validates barcode uniqueness. */
    suspend fun save(product: ProductEntity): Long {
        val barcode = product.barcode?.trim()?.takeIf { it.isNotEmpty() }
        if (barcode != null) {
            val existing = dao.getByBarcode(barcode)
            if (existing != null && existing.id != product.id) {
                throw IllegalArgumentException("Barcode already used by \"${existing.name}\"")
            }
        }
        if (product.isBreakBulk) {
            if (product.id != 0L && dao.getChildren(product.id).isNotEmpty()) {
                throw IllegalArgumentException("Unlink existing tingi items before changing this bulk item into a tingi item")
            }
            val parentId = product.parentProductId
                ?: throw IllegalArgumentException("Choose the bulk item this is opened from")
            if (parentId == product.id) throw IllegalArgumentException("An item cannot be its own parent")
            val parent = dao.getById(parentId) ?: throw IllegalArgumentException("Parent item not found")
            if (parent.isBreakBulk) throw IllegalArgumentException("Parent item must be a bulk (non-tingi) item")
            if (product.conversionRate < 2) throw IllegalArgumentException("Pieces per bulk unit must be at least 2")
        }
        val clean = product.copy(
            name = product.name.trim(),
            barcode = barcode,
            parentProductId = if (product.isBreakBulk) product.parentProductId else null,
            conversionRate = if (product.isBreakBulk) product.conversionRate else 1
        )
        return if (clean.id == 0L) {
            dao.insert(clean)
        } else {
            dao.update(clean)
            clean.id
        }
    }

    /** Deletes a product. Any tingi items opened from it become regular items. */
    suspend fun delete(product: ProductEntity) {
        db.withTransaction {
            dao.unlinkChildren(product.id)
            dao.delete(product)
        }
    }

    suspend fun restock(productId: Long, addQty: Int) {
        db.withTransaction {
            val p = dao.getById(productId) ?: return@withTransaction
            dao.update(p.copy(stockQuantity = (p.stockQuantity + addQty).coerceAtLeast(0)))
        }
    }

    /** Manually opens [units] parent units into loose pieces of a break-bulk item. */
    suspend fun unbox(childId: Long, units: Int = 1) {
        db.withTransaction {
            val child = dao.getById(childId) ?: throw IllegalStateException("Item not found")
            val parentId = child.parentProductId
            if (!child.isBreakBulk || parentId == null) throw IllegalStateException("Not a tingi item")
            val parent = dao.getById(parentId) ?: throw IllegalStateException("Bulk item not found")
            if (parent.stockQuantity < units) throw IllegalStateException("No more \"${parent.name}\" to open")
            dao.update(parent.copy(stockQuantity = parent.stockQuantity - units))
            dao.update(child.copy(stockQuantity = child.stockQuantity + units * child.conversionRate))
        }
    }
}
