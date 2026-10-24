package com.sarisari.pos.domain

import com.sarisari.pos.data.local.entity.ProductEntity

/**
 * Pure "patingi-tingi" logic: no Android or database dependencies, so it is unit-testable.
 *
 * A break-bulk child (e.g. a cigarette stick) can be sold from its own loose stock first;
 * when loose stock runs out, whole parent units (packs) are opened automatically.
 */
object BreakBulk {

    data class Deduction(
        val newChildStock: Int,
        val newParentStock: Int?,
        val parentUnitsOpened: Int
    )

    /** Number of parent units that must be opened to sell [qty] of [child]. */
    fun unitsToOpen(child: ProductEntity, qty: Int): Int {
        if (!child.isBreakBulk || child.parentProductId == null) return 0
        val shortfall = qty - child.stockQuantity
        if (shortfall <= 0) return 0
        val rate = child.conversionRate.coerceAtLeast(1)
        return (shortfall + rate - 1) / rate
    }

    /** Total units of [product] that can be sold, counting unopened parent stock. */
    fun available(product: ProductEntity, parent: ProductEntity?): Int {
        if (!product.isBreakBulk || parent == null) return product.stockQuantity
        return product.stockQuantity + parent.stockQuantity.coerceAtLeast(0) * product.conversionRate.coerceAtLeast(1)
    }

    /**
     * Plans the stock change for selling [qty] of [product].
     * Returns null when there is not enough stock, even after opening parent units.
     */
    fun planDeduction(product: ProductEntity, parent: ProductEntity?, qty: Int): Deduction? {
        require(qty > 0) { "Quantity must be positive" }
        if (product.stockQuantity >= qty) {
            return Deduction(product.stockQuantity - qty, parent?.stockQuantity, 0)
        }
        if (!product.isBreakBulk || parent == null) return null
        val open = unitsToOpen(product, qty)
        if (parent.stockQuantity < open) return null
        val rate = product.conversionRate.coerceAtLeast(1)
        return Deduction(
            newChildStock = product.stockQuantity + open * rate - qty,
            newParentStock = parent.stockQuantity - open,
            parentUnitsOpened = open
        )
    }

    /**
     * Checks whether every line in a cart (productId -> qty) can be fulfilled at once,
     * taking into account that a bulk parent and its loose children share parent stock.
     */
    fun isCartFeasible(cart: Map<Long, Int>, productsById: Map<Long, ProductEntity>): Boolean {
        val parentUnitsUsed = HashMap<Long, Int>()
        for ((id, qty) in cart) {
            if (qty <= 0) continue
            val product = productsById[id] ?: return false
            val parentId = product.parentProductId
            if (product.isBreakBulk && parentId != null && productsById.containsKey(parentId)) {
                parentUnitsUsed[parentId] = (parentUnitsUsed[parentId] ?: 0) + unitsToOpen(product, qty)
            } else if (qty > product.stockQuantity) {
                return false
            }
        }
        for ((parentId, opened) in parentUnitsUsed) {
            val parent = productsById[parentId] ?: return false
            val soldDirectly = cart[parentId] ?: 0
            if (opened + soldDirectly > parent.stockQuantity) return false
        }
        return true
    }
}
