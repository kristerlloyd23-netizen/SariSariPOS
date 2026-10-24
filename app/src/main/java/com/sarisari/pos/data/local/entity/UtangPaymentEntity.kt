package com.sarisari.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "utang_payments", indices = [Index(value = ["customerId"])])
data class UtangPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val amountPaid: Double,
    val timestamp: Long = System.currentTimeMillis()
)
