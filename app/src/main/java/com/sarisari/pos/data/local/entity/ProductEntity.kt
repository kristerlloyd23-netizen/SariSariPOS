package com.sarisari.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A sellable item.
 *
 * Break-bulk ("patingi-tingi") items have [isBreakBulk] = true and point to the bulk
 * product they are opened from via [parentProductId]. [conversionRate] is how many of
 * this item come out of ONE parent unit (e.g. 1 pack of Marlboro = 20 sticks).
 */
@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"]), Index(value = ["category"])]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val barcode: String? = null,
    val costPrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Int,
    val lowStockThreshold: Int = 5,
    val isBreakBulk: Boolean = false,
    val parentProductId: Long? = null,
    val conversionRate: Int = 1,
    /** Relative app-private file path; never store bitmap data in the database. */
    val imagePath: String? = null,
    /** Product-page URL to show attribution for a catalog photo; null for owner photos. */
    val imageSource: String? = null
) {
    val isLowStock: Boolean get() = stockQuantity <= lowStockThreshold
}
