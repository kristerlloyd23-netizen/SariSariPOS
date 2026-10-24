package com.sarisari.pos.domain

import com.sarisari.pos.data.local.entity.ProductEntity

data class CartLine(
    val product: ProductEntity,
    val quantity: Int
) {
    val subtotal: Double get() = product.sellingPrice * quantity
}
