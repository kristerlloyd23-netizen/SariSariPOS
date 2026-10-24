package com.sarisari.pos.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 1234.5 -> "₱1,234.50". Uses a fresh formatter because NumberFormat is not thread-safe. */
fun Double.toPeso(): String = "₱" + NumberFormat.getNumberInstance(Locale.US).run {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
    format(this@toPeso)
}

/** Parses user input like "1,234.50" or "₱20" into a Double, or null. */
fun String.parseAmount(): Double? =
    replace("₱", "").replace(",", "").trim().toDoubleOrNull()

fun Long.toDateTime(): String =
    SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US).format(Date(this))

fun Long.toShortDay(): String =
    SimpleDateFormat("EEE", Locale.US).format(Date(this))
