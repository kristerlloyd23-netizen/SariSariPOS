package com.sarisari.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A "suki" with a store-credit (utang) account. */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String? = null,
    val creditLimit: Double = 1000.0,
    val currentBalance: Double = 0.0
) {
    val remainingCredit: Double get() = (creditLimit - currentBalance).coerceAtLeast(0.0)
}
