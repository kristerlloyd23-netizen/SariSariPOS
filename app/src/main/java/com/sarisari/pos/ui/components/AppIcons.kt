package com.sarisari.pos.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Small hand-built icons so we don't need the very large material-icons-extended artifact.
 * All are drawn on a 24x24 grid from simple rectangles.
 */
object AppIcons {

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply { path(fill = SolidColor(Color.Black), pathBuilder = block) }
            .build()

    private fun PathBuilder.rect(x: Float, y: Float, w: Float, h: Float) {
        moveTo(x, y); horizontalLineTo(x + w); verticalLineTo(y + h); horizontalLineTo(x); close()
    }

    val Minus: ImageVector by lazy { icon("Minus") { rect(5f, 11f, 14f, 2f) } }

    /** Barcode inside scanner corner brackets. */
    val BarcodeScan: ImageVector by lazy {
        icon("BarcodeScan") {
            // corners
            rect(2f, 2f, 6f, 2f); rect(2f, 2f, 2f, 6f)
            rect(16f, 2f, 6f, 2f); rect(20f, 2f, 2f, 6f)
            rect(2f, 20f, 6f, 2f); rect(2f, 16f, 2f, 6f)
            rect(16f, 20f, 6f, 2f); rect(20f, 16f, 2f, 6f)
            // bars
            rect(6f, 7f, 1.5f, 10f); rect(8.5f, 7f, 1f, 10f); rect(10.5f, 7f, 2f, 10f)
            rect(13.5f, 7f, 1f, 10f); rect(15.5f, 7f, 2.5f, 10f)
        }
    }

    /** Stacked boxes for inventory. */
    val Inventory: ImageVector by lazy {
        icon("Inventory") {
            rect(3f, 12f, 8f, 9f); rect(13f, 12f, 8f, 9f); rect(8f, 3f, 8f, 8f)
        }
    }

    /** Bar chart for reports. */
    val BarChart: ImageVector by lazy {
        icon("BarChart") {
            rect(3f, 13f, 4f, 8f); rect(10f, 8f, 4f, 13f); rect(17f, 3f, 4f, 18f); rect(2f, 21f, 20f, 1.5f)
        }
    }

    /** Notebook / ledger for utang. */
    val Ledger: ImageVector by lazy {
        icon("Ledger") {
            rect(5f, 2f, 15f, 2f); rect(5f, 20f, 15f, 2f); rect(18f, 2f, 2f, 20f); rect(5f, 2f, 2f, 20f)
            rect(9f, 7f, 7f, 1.6f); rect(9f, 11f, 7f, 1.6f); rect(9f, 15f, 5f, 1.6f)
            rect(3f, 6f, 3f, 1.6f); rect(3f, 11f, 3f, 1.6f); rect(3f, 16f, 3f, 1.6f)
        }
    }

    /** QR-code glyph for e-wallets. */
    val Qr: ImageVector by lazy {
        icon("Qr") {
            // three finder squares (outlined by drawing 4 sides)
            fun finder(x: Float, y: Float) {
                rect(x, y, 8f, 1.8f); rect(x, y + 6.2f, 8f, 1.8f)
                rect(x, y, 1.8f, 8f); rect(x + 6.2f, y, 1.8f, 8f)
                rect(x + 3f, y + 3f, 2f, 2f)
            }
            finder(2f, 2f); finder(14f, 2f); finder(2f, 14f)
            rect(14f, 14f, 3f, 3f); rect(18f, 18f, 3f, 3f); rect(18f, 14f, 3f, 2f); rect(14f, 19f, 3f, 2f)
        }
    }

    /** Peso-coin cash icon. */
    val Cash: ImageVector by lazy {
        icon("Cash") {
            rect(2f, 6f, 20f, 2f); rect(2f, 16f, 20f, 2f); rect(2f, 6f, 2f, 12f); rect(20f, 6f, 2f, 12f)
            rect(10f, 9f, 2f, 6f); rect(10f, 9f, 4f, 1.6f); rect(13f, 9f, 1.6f, 3.4f); rect(10f, 11.6f, 4f, 1.4f)
            rect(9f, 10.2f, 6f, 0.8f)
        }
    }

    /** Open-box glyph for unboxing break-bulk items. */
    val OpenBox: ImageVector by lazy {
        icon("OpenBox") {
            rect(4f, 10f, 16f, 2f); rect(4f, 10f, 2f, 11f); rect(18f, 10f, 2f, 11f); rect(4f, 19f, 16f, 2f)
            rect(2f, 6f, 7f, 2f); rect(15f, 6f, 7f, 2f); rect(11f, 3f, 2f, 7f)
        }
    }

    /** Chat bubble for the grounded store assistant. */
    val Assistant: ImageVector by lazy {
        icon("Assistant") {
            rect(3f, 3f, 18f, 2f); rect(3f, 3f, 2f, 13f); rect(19f, 3f, 2f, 13f)
            rect(3f, 14f, 12f, 2f); rect(8f, 16f, 2f, 4f); rect(8f, 19f, 5f, 2f)
            rect(7f, 8f, 2f, 2f); rect(11f, 8f, 2f, 2f); rect(15f, 8f, 2f, 2f)
        }
    }

}
