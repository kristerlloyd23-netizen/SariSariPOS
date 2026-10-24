package com.sarisari.pos.domain

enum class PaymentType(val dbValue: String, val label: String) {
    CASH("CASH", "Cash"),
    UTANG("UTANG", "Utang"),
    GCASH("GCASH", "GCash/Maya");

    companion object {
        fun fromDb(value: String): PaymentType =
            entries.firstOrNull { it.dbValue == value } ?: CASH
    }
}
