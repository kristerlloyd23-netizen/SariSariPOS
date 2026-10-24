package com.sarisari.pos.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.sarisari.pos.data.local.entity.TransactionEntity
import com.sarisari.pos.data.local.entity.TransactionItemEntity

/** Revenue grouped by payment type. */
data class PaymentSummary(
    val paymentType: String,
    val total: Double,
    val count: Int
)

/** Best sellers for a period. */
data class TopProduct(
    val productId: Long,
    val productName: String,
    val qty: Int,
    val revenue: Double
)

/** Lightweight row for charts. */
data class SalePoint(
    val timestamp: Long,
    val totalAmount: Double
)

data class TransactionWithItems(
    @Embedded val transaction: TransactionEntity,
    @Relation(parentColumn = "id", entityColumn = "transactionId")
    val items: List<TransactionItemEntity>
)
